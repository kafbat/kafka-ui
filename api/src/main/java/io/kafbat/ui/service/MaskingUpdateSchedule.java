package io.kafbat.ui.service;

import io.kafbat.ui.client.ConfluentApiClient;
import io.kafbat.ui.mapper.DynamicConfigMapper;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaClustersInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacRolesInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO;
import io.kafbat.ui.model.sainsburys.Entity;
import io.kafbat.ui.model.sainsburys.EntityAttributes;
import io.kafbat.ui.model.sainsburys.SchemaMetadataResponse;
import io.kafbat.ui.repository.DynamoRbacEntityRepository;
import io.kafbat.ui.util.ApplicationRestarter;
import io.kafbat.ui.util.DynamicConfigOperations;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class MaskingUpdateSchedule {

  private final ConfluentApiClient confluentApiClient;
  private final DynamicConfigOperations dynamicConfigOperations;
  private final DynamicConfigMapper configMapper;
  private final ApplicationRestarter restarter;
  private final DynamoRbacEntityRepository dynamoRbacEntityRepository;

  @Value("${kit.masking.rule.chars-replacement: X, x, x, - }")
  private List<String> defaultMaskingCharsReplacement;

  @Value("${kit.masking.rule.topic-replacement: [CONFIDENTIAL] }")
  private String defaultMaskingTopicReplacement;

  @Value("${kit.masking.rule.tags.topic: highly_sensitive_personal_data_pii_customer, highly_sensitive_topic_personal_data_pii_customer }")
  private List<String> defaultTopicTag;

  @Value("${kit.masking.date.format: yyyy-MM-dd HH:mm:ss.SSS }")
  private String dateFormat;

  public MaskingUpdateSchedule(ConfluentApiClient confluentApiClient,
                               DynamicConfigOperations dynamicConfigOperations, DynamicConfigMapper configMapper,
                               ApplicationRestarter restarter, DynamoRbacEntityRepository dynamoRbacEntityRepository) {
    this.confluentApiClient = confluentApiClient;
    this.dynamicConfigOperations = dynamicConfigOperations;
    this.configMapper = configMapper;
    this.restarter = restarter;
    this.dynamoRbacEntityRepository = dynamoRbacEntityRepository;
  }

  @Scheduled(fixedRateString = "${kit.masking.scheduler.update-masking-tags-rate-millis:30000}", initialDelay = 10000)
  private void executeMasking(){
    try{
      log.info("Update masking tags dynamic config start");
      AtomicBoolean isMetadataUpdated = new AtomicBoolean(false);
      ApplicationConfigPropertiesDTO config = configMapper.toDto(dynamicConfigOperations.getCurrentProperties());
      ApplicationConfigPropertiesKafkaDTO kafkaDTO = config.getKafka();

      defaultTopicTag.forEach(tag->{
        SchemaMetadataResponse confluentResponse = metadataTopicResponses(tag);
        List<EntityAttributes> confluentTopicList = confluentResponse.getEntities().stream()
            .filter(e->e.getClassificationNames().contains(tag))
            .map(Entity::getAttributes).filter(Objects::nonNull)
            .toList();
        confluentTopicList.forEach(topic->{
          kafkaDTO.getClusters().stream()
              .filter(c-> topic.getQualifiedName() != null &&
                  topic.getQualifiedName().contains(c.getName()))
              .forEach(cluster ->{
                if(cluster.getMasking().isEmpty()){
                  if(tag.contains("topic")){
                    updateTopicLevelMasking(cluster, topic.getName(), new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated);
                  }else {
                    updateFieldLevelMasking(cluster, topic.getName(),
                        new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated);
                  }
                }
                else {
                  if (tag.contains("topic")) {
                  List<@Valid ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO> topicMaskList =
                      cluster.getMasking().stream()
                          .filter(mask -> mask.getType().equals(
                              ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.REPLACE))
                          .filter(mask->mask.getTopicValuesPattern().equalsIgnoreCase(topic.getName()))
                          .toList();

                  if (!topicMaskList.isEmpty()) {
                    topicMaskList.forEach(mask -> {
                      updateTopicLevelMasking(cluster, topic.getName(), mask, isMetadataUpdated);
                    });
                  } else {
                    updateTopicLevelMasking(cluster, topic.getName(),
                        new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated);
                  }
                }else{
                    List<@Valid ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO> fieldMaskList = cluster.getMasking().stream()
                        .filter(mask -> mask.getType().equals(
                            ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.MASK))
                        .filter(mask->mask.getTopicValuesPattern().equalsIgnoreCase(topic.getName()))
                        .toList();

                    if(!fieldMaskList.isEmpty()){
                      fieldMaskList.forEach(mask -> {
                        updateFieldLevelMasking(cluster, topic.getName(), mask, isMetadataUpdated);
                      });
                    }else{
                      updateFieldLevelMasking(cluster, topic.getName(), new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated);
                    }
                  }
                }
              });

        });
      });

      if(isMetadataUpdated.get()){
        log.info("Persist cluster config change");
        var newConfig = configMapper.fromDto(config);
        dynamicConfigOperations.persist(newConfig);
        restarter.requestRestart();
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }
  private void updateTopicLevelMasking(ApplicationConfigPropertiesKafkaClustersInnerDTO cluster, String topic,
                                       ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO mask,
                                       AtomicBoolean isMetadataUpdated) {
          log.info("Topic Level for topic name: {}", topic);
          SchemaMetadataResponse confluentTopicFieldsResponse = metadataTopicFieldsResponses(topic);
          List<String> confluentTopicFieldsList = confluentTopicFieldsResponse.getEntities().stream()
              .map(Entity::getAttributes).filter(Objects::nonNull)
              .map(EntityAttributes::getName)
              .toList();
          if (!confluentTopicFieldsList.isEmpty()) {
            log.info("Processing fields from confluent: {}", confluentTopicFieldsList);
              mask.type(ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.REPLACE);
              mask.setReplacement(defaultMaskingTopicReplacement);
              mask.setTopicValuesPattern(topic);
              List<String> fieldsToMask = mask.getFields().isEmpty()? confluentTopicFieldsList : fieldsToMask(mask.getFields(), confluentTopicFieldsList);
              if (!fieldsToMask.isEmpty()) {
                fieldsToMask.stream().forEach(field -> {
                  mask.addFieldsItem(field);
                  isMetadataUpdated.set(true);
                });
              }
              List<String> fieldsToRemove = mask.getFields().isEmpty()? mask.getFields() : fieldsToRemoveFromMask(mask.getFields(), confluentTopicFieldsList);
              if (!fieldsToRemove.isEmpty()) {
                fieldsToRemove.stream().forEach(field -> {
                  mask.getFields().remove(field);
                  isMetadataUpdated.set(true);
                });
              }
              if(!cluster.getMasking().contains(mask)){
                cluster.getMasking().add(mask);
              }

          }
  }

  private void updateFieldLevelMasking(ApplicationConfigPropertiesKafkaClustersInnerDTO cluster, String topic,
                         ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO mask,
                         AtomicBoolean isMetadataUpdated) {
    //todo at this level we have the cluster and topic name we can call Confluent API for PII fields
    List<String> currentFields = mask.getFields();
    log.info("Topic Level for topic name: {}", topic);
    SchemaMetadataResponse confluentResponse = metadataResponses(cluster.getName(), topic);
    List<String> confluentEntityList = confluentResponse.getEntities().stream()
        .map(Entity::getAttributes).filter(Objects::nonNull)
        .map(EntityAttributes::getName)
        .toList();
    if(!confluentEntityList.isEmpty()){
      log.info("Processing fields from confluent: {}", confluentEntityList);
      mask.type(ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.MASK);
      mask.setMaskingCharsReplacement(defaultMaskingCharsReplacement);
      mask.setTopicValuesPattern(topic);
      List<String> fieldsToMask = currentFields.isEmpty() ? confluentEntityList : fieldsToMask(currentFields, confluentEntityList);
      if(!fieldsToMask.isEmpty()){
        fieldsToMask.stream().forEach(field ->{
          mask.addFieldsItem(field);
          isMetadataUpdated.set(true);
        });
      }
      List<String> fieldsToRemove = currentFields.isEmpty() ? currentFields : fieldsToRemoveFromMask(currentFields, confluentEntityList);
      if(!fieldsToRemove.isEmpty()){
        fieldsToRemove.stream().forEach(field->{
          mask.getFields().remove(field);
          isMetadataUpdated.set(true);
        });
      }
      if(!cluster.getMasking().contains(mask)){
        cluster.getMasking().add(mask);
      }
    }
  }

  @Scheduled(fixedRateString = "${kit.masking.scheduler.update-rbac-rate-millis:30000}", initialDelay = 10000)
  private void executeUnmaskingRBACRevoke(){
    try{
      log.info("Revoke expired user unmasking rbac config start");
      AtomicBoolean isSubjectsRevoked = new AtomicBoolean(false);
      ApplicationConfigPropertiesDTO config = configMapper.toDto(dynamicConfigOperations.getCurrentProperties());
      ApplicationConfigPropertiesRbacDTO rbacDTO = config.getRbac();
      List<ApplicationConfigPropertiesRbacRolesInnerDTO> rolesToRevoke = new ArrayList<>();
      rbacDTO.getRoles().stream()
          .forEach(role->{
            List<ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO>  subjectsToRevoke = role.getSubjects().stream()
                  .filter(s->s.getExpiryTime() != null)
                  .filter(s->strToDateTime(s.getExpiryTime()).before(Calendar.getInstance().getTime()))
                  .toList();
            if(!subjectsToRevoke.isEmpty()){
              rolesToRevoke.add(role);
            }
      });
      if(!rolesToRevoke.isEmpty()){
        rbacDTO.getRoles().removeAll(rolesToRevoke);
        isSubjectsRevoked.set(true);
      }

      if(isSubjectsRevoked.get()){
        log.info("Persist RBAC config change");
        var newConfig = configMapper.fromDto(config);
        dynamicConfigOperations.persist(newConfig);
        restarter.requestRestart();
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }


  private List<String> fieldsToMask(List<String> dynamicConfigFields, List<String> schemaRegistryFields){
    return  schemaRegistryFields.stream()
        .filter(item -> !dynamicConfigFields.contains(item))
        .toList();
  }
  private List<String> fieldsToRemoveFromMask(List<String> dynamicConfigFields, List<String> schemaRegistryFields){
    return dynamicConfigFields.stream()
        .filter(item -> !schemaRegistryFields.contains(item))
        .toList();
  }

  private SchemaMetadataResponse metadataResponses(String cluster, String topic){
    try{
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveSchemaMetadata();
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
    return null;
  }

  private SchemaMetadataResponse metadataTopicResponses(String tag){
    try{
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveTopicMetadata(tag);
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
    return null;
  }

  private SchemaMetadataResponse metadataTopicFieldsResponses(String tag){
    try{
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveTopicFieldsMetadata();
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
    return null;
  }

  private Date strToDateTime(String date){
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    try {
      return sdf.parse(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
