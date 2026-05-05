package io.kafbat.ui.service.sainsburys;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.kafbat.ui.client.ConfluentApiClient;
import io.kafbat.ui.config.sainsburys.ConfluentAuthConfig;
import io.kafbat.ui.mapper.DynamicConfigMapper;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaClustersInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacRolesInnerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO;
import io.kafbat.ui.model.AuthenticateRequestDTO;
import io.kafbat.ui.model.sainsburys.confluent.ConfluentAvroField;
import io.kafbat.ui.model.sainsburys.confluent.ConfluentAvroSchema;
import io.kafbat.ui.model.sainsburys.confluent.Entity;
import io.kafbat.ui.model.sainsburys.confluent.EntityAttributes;
import io.kafbat.ui.model.sainsburys.confluent.SchemaMetadataResponse;
import io.kafbat.ui.model.sainsburys.confluent.SubjectMetadataResponse;
import io.kafbat.ui.model.sainsburys.confluent.TagDefinitionClassificationResponse;
import io.kafbat.ui.model.sainsburys.dynamo.DynamoMaskingEntity;
import io.kafbat.ui.repository.DynamoMaskingEntityRepository;
import io.kafbat.ui.util.DynamicConfigOperations;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@EnableRetry
public class MaskingUpdateSchedule {

  private final ConfluentApiClient confluentApiClient;
  private final DynamicConfigOperations dynamicConfigOperations;
  private final DynamicConfigMapper configMapper;
  private final DynamoClusterProperties dynamoClusterProperties;
  private final DynamoMaskingEntityRepository dynamoMaskingEntityRepository;

  @Value("${kit.masking.feature.enabled: false }")
  private boolean isMaskingEnabled;

  @Value("${kit.masking.rule.chars-replacement: X, x, x, - }")
  private List<String> defaultMaskingCharsReplacement;

  @Value("${kit.masking.rule.topic-replacement: [CONFIDENTIAL] }")
  private String defaultMaskingTopicReplacement;

  @Value("${kit.masking.rule.tags.schema-field: sainsburys.dataClassification }")
  private String schemaDataClassificationTag;

  @Value("${kit.masking.date.format: yyyy-MM-dd HH:mm:ss.SSS }")
  private String dateFormat;

  public MaskingUpdateSchedule(ConfluentApiClient confluentApiClient,
                               DynamicConfigOperations dynamicConfigOperations, DynamicConfigMapper configMapper,
                               DynamoClusterProperties dynamoClusterProperties,
                               DynamoMaskingEntityRepository dynamoMaskingEntityRepository) {
    this.confluentApiClient = confluentApiClient;
    this.dynamicConfigOperations = dynamicConfigOperations;
    this.configMapper = configMapper;
    this.dynamoClusterProperties = dynamoClusterProperties;
    this.dynamoMaskingEntityRepository = dynamoMaskingEntityRepository;
  }

  @Scheduled(fixedRateString = "${kit.masking.scheduler.update-masking-tags-rate-millis:3000000}", initialDelay = 10000)
  protected void executeMasking(){
    try{
      if(isMaskingEnabled){
        log.info("Update masking tags dynamic config start");
        AtomicBoolean isMetadataUpdated = new AtomicBoolean(false);
        ApplicationConfigPropertiesDTO config = configMapper.toDto(dynamicConfigOperations.getCurrentProperties());
        ApplicationConfigPropertiesKafkaDTO kafkaDTO = config.getKafka();
        kafkaDTO.getClusters()
            .stream().filter(c->!c.getName().contains("unmasked-"))
            .forEach(cluster->{
              String clusterBaseUrl = cluster.getSchemaRegistry();
              AuthenticateRequestDTO clusterAuth =cluster.getSchemaRegistryAuth();
              List<TagDefinitionClassificationResponse> tagDefinitionList = tagDefinitionResponse(clusterBaseUrl, clusterAuth);
              if(tagDefinitionList != null && !tagDefinitionList.isEmpty()){
                tagDefinitionList.stream().map(TagDefinitionClassificationResponse::getName)
                    .forEach(tag ->{
                      log.info("Tag found for cluster: {}, tag: {}", cluster.getName(), tag);
                      maskProcessor(cluster, clusterAuth, tag, isMetadataUpdated);
//                      maskProcessor(clusterBaseUrl, clusterAuth, tag, kafkaDTO, isMetadataUpdated);
                    });
              }
            });

        if(isMetadataUpdated.get()){
          log.info("Persist cluster config change");
//        var newConfig = configMapper.fromDto(config);
//        dynamicConfigOperations.persist(newConfig);
          log.info("DynamoDB Masking Config Refresh");
          dynamoClusterProperties.loadMaskingConfiguration();

        }
      }else{
        log.info("Masking feature not enabled as yet, configure in Application properties.");
      }
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  private void maskProcessor(ApplicationConfigPropertiesKafkaClustersInnerDTO cluster, AuthenticateRequestDTO authentication, String tag , AtomicBoolean isMetadataUpdated) {
    String baseUrl = cluster.getSchemaRegistry();
    SchemaMetadataResponse confluentResponse = metadataTopicResponses(baseUrl, authentication, tag);
    if(confluentResponse == null){
      log.error("Tag metadata API did not return correctly for baseUrl: {} and tag: {}", baseUrl, tag);
      return;
    }
    List<EntityAttributes> confluentTopicList = confluentResponse.getEntities().stream()
        .filter(e->e.getClassificationNames().contains(tag))
        .map(Entity::getAttributes).filter(Objects::nonNull)
        .toList();
    confluentTopicList.forEach(topic->{
      if(topic.getQualifiedName() != null &&
          topic.getQualifiedName().contains(getLogicalCLusterFromProperties(cluster))){
        if(cluster.getMasking().isEmpty()){

          SubjectMetadataResponse confluentTopicFieldsResponse = retrieveSubjectMetadataResponses(baseUrl, authentication, topic.getName());

          if(confluentTopicFieldsResponse.getSchema().contains(schemaDataClassificationTag)){
            updateFieldLevelMasking(cluster, topic.getName(),
                new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated, confluentTopicFieldsResponse);
          }else{
            updateTopicLevelMasking(cluster, topic.getName(), new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(),
                isMetadataUpdated, confluentTopicFieldsResponse);
          }
        }
        else {
          SubjectMetadataResponse confluentTopicFieldsResponse = retrieveSubjectMetadataResponses(baseUrl, authentication, topic.getName());

          if(confluentTopicFieldsResponse.getSchema().contains(schemaDataClassificationTag)){
            List<@Valid ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO> fieldMaskList = cluster.getMasking().stream()
                .filter(mask -> mask.getType().equals(
                    ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.MASK))
                .filter(mask->mask.getTopicValuesPattern().equalsIgnoreCase(topic.getName()))
                .toList();

            if(!fieldMaskList.isEmpty()){
              fieldMaskList.forEach(mask -> {
                updateFieldLevelMasking(cluster, topic.getName(), mask, isMetadataUpdated, confluentTopicFieldsResponse);
              });
            }else{
              updateFieldLevelMasking(cluster, topic.getName(), new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(),
                  isMetadataUpdated, confluentTopicFieldsResponse);
            }
          }else{
            List<@Valid ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO> topicMaskList =
                cluster.getMasking().stream()
                    .filter(mask -> mask.getType().equals(
                        ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.REPLACE))
                    .filter(mask->mask.getTopicValuesPattern().equalsIgnoreCase(topic.getName()))
                    .toList();

            if (!topicMaskList.isEmpty()) {
              topicMaskList.forEach(mask -> {
                updateTopicLevelMasking(cluster, topic.getName(), mask, isMetadataUpdated, confluentTopicFieldsResponse);
              });
            } else {
              updateTopicLevelMasking(cluster, topic.getName(),
                  new ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO(), isMetadataUpdated, confluentTopicFieldsResponse);
            }
          }
        }
      }

    });
  }

  private static String getLogicalCLusterFromProperties(ApplicationConfigPropertiesKafkaClustersInnerDTO c) {
    return  String.valueOf(c.getProperties().get("clusterId"));
  }

  private void updateTopicLevelMasking(ApplicationConfigPropertiesKafkaClustersInnerDTO cluster, String topic,
                                       ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO mask,
                                       AtomicBoolean isMetadataUpdated, SubjectMetadataResponse confluentTopicFieldsResponse) {
          log.info("Topic Level Masking for topic name: {}", topic);
          ConfluentAvroSchema confluentAvroSchema = avroSchemaMapper(confluentTopicFieldsResponse.getSchema());

          if (confluentAvroSchema != null && !confluentAvroSchema.getFields().isEmpty()) {

            log.info("Processing fields from confluent subject: {}", confluentTopicFieldsResponse.getSubject());
              mask.type(ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO.TypeEnum.REPLACE);
              mask.setReplacement(defaultMaskingTopicReplacement);
              mask.setTopicValuesPattern(topic);
              List<String> confluentTopicFieldsList = confluentAvroSchema.getFields()
                  .stream()
                  .map(ConfluentAvroField::getName)
                  .map(String::toLowerCase)
                  .toList();
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
                saveMaskingEntity(mapperMaskingDtoToEntity(cluster.getName(), mask));
              }

          }
  }

  private void updateFieldLevelMasking(ApplicationConfigPropertiesKafkaClustersInnerDTO cluster, String topic,
                         ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO mask,
                         AtomicBoolean isMetadataUpdated, SubjectMetadataResponse confluentTopicFieldsResponse) {
    List<String> currentFields = mask.getFields();
    log.info("Field Level Masking for topic name: {}", topic);
    ConfluentAvroSchema confluentAvroSchema = avroSchemaMapper(confluentTopicFieldsResponse.getSchema());

    List<String> confluentEntityList = confluentAvroSchema.getFields().stream()
        .filter(f-> !f.getCustomTags().isEmpty() && f.getCustomTags().get(schemaDataClassificationTag) != null)
        .map(ConfluentAvroField::getName)
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
        saveMaskingEntity(mapperMaskingDtoToEntity(cluster.getName(), mask));
      }
    }
  }

  @Scheduled(fixedRateString = "${kit.masking.scheduler.update-rbac-rate-millis:3000000}", initialDelay = 10000)
  protected void executeUnmaskingRBACRevoke(){
    try{
      if(isMaskingEnabled){
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
//        var newConfig = configMapper.fromDto(config);
//        dynamicConfigOperations.persist(newConfig);
        }
      }else{
        log.info("Masking feature not enabled as yet, configure in Application properties.");
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

  @Retryable(
      retryFor = { FeignException.class },
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  private List<TagDefinitionClassificationResponse> tagDefinitionResponse(String baseUrl, AuthenticateRequestDTO authentication){
    try{
      String authorization = ConfluentAuthConfig.generateBasicAuthentication(authentication.getUsername(),
          authentication.getPassword());
      ResponseEntity<List<TagDefinitionClassificationResponse>> tagDefinitions = confluentApiClient.retrieveTagDefinitions(URI.create(baseUrl), authorization);
      if(tagDefinitions != null && tagDefinitions.getStatusCode().is2xxSuccessful()){
        return tagDefinitions.getBody();
      }
    }catch (FeignException e){
      log.error("Feign API call error with message: {}", e.getMessage());

    }
    return null;
  }

  private SchemaMetadataResponse metadataResponses(String baseUrl, AuthenticateRequestDTO authentication, String cluster, String topic){
    try{
      String authorization = ConfluentAuthConfig.generateBasicAuthentication(authentication.getUsername(),
          authentication.getPassword());
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveSchemaMetadata(URI.create(baseUrl), authorization);
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (Exception e){
      log.error("Feign API call error with message: {}", e.getMessage());
    }
    return null;
  }

  @Retryable(
      retryFor = { FeignException.class },
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  private SchemaMetadataResponse metadataTopicResponses(String baseUrl, AuthenticateRequestDTO authentication, String tag){
    try{
      String authorization = ConfluentAuthConfig.generateBasicAuthentication(authentication.getUsername(),
          authentication.getPassword());
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveTopicMetadata(URI.create(baseUrl),
          authorization, tag);
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (FeignException e){
      log.error("Feign API call error with message: {}", e.getMessage());

    }
    return null;
  }

  private SchemaMetadataResponse metadataTopicFieldsResponses(String baseUrl, AuthenticateRequestDTO authentication, String tag){
    try{
      String authorization = ConfluentAuthConfig.generateBasicAuthentication(authentication.getUsername(),
          authentication.getPassword());
      ResponseEntity<SchemaMetadataResponse> metadata = confluentApiClient.retrieveTopicFieldsMetadata(URI.create(baseUrl), authorization, tag);
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (FeignException e){
      log.error("Feign API call error with message: {}", e.getMessage());

    }
    return null;
  }

  @Retryable(
      retryFor = { FeignException.class },
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  private SubjectMetadataResponse retrieveSubjectMetadataResponses(String baseUrl, AuthenticateRequestDTO authentication, String topic){
    try{
      String authorization = ConfluentAuthConfig.generateBasicAuthentication(authentication.getUsername(),
          authentication.getPassword());
      ResponseEntity<SubjectMetadataResponse> metadata = confluentApiClient.retrieveSubjectMetadata(URI.create(baseUrl), authorization, topic);
      if(metadata != null && metadata.getStatusCode().is2xxSuccessful()){
        return metadata.getBody();
      }
    }catch (FeignException e){
      log.error("Feign API call error with message: {}", e.getMessage());
    }
    return null;
  }

  private ConfluentAvroSchema avroSchemaMapper(String schema){
    ObjectMapper mapper = new ObjectMapper();
    try{
      return mapper.readValue(schema, ConfluentAvroSchema.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private Date strToDateTime(String date){
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    try {
      return sdf.parse(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
  @Retryable(
      retryFor = {
          ProvisionedThroughputExceededException.class,
          SdkClientException.class
      },
      backoff = @Backoff(delay = 500, multiplier = 2)
  )
  private void saveMaskingEntity(DynamoMaskingEntity mask){
    try {
      dynamoMaskingEntityRepository.save(mask);
    } catch (Exception e) {
      log.error("Dynamo Masking config failed with message: {}", e.getMessage());
    }
  }

  private DynamoMaskingEntity mapperMaskingDtoToEntity(String cluster, ApplicationConfigPropertiesKafkaClustersInnerMaskingInnerDTO source){
    DynamoMaskingEntity target = new DynamoMaskingEntity();
    if(source.getTopicValuesPattern() != null){
      target.setName(String.format("%s_%s", cluster, source.getTopicValuesPattern()));
    } else {
      target.setName(String.format("%s_%s", cluster, source.getTopicKeysPattern()));
    }
    target.setType(source.getType().getValue());
    target.setReplacement(source.getReplacement());
    target.setMaskingCharsReplacement(source.getMaskingCharsReplacement());
    target.setTopicKeysPattern(source.getTopicKeysPattern());
    target.setTopicValuesPattern(source.getTopicValuesPattern());
    target.setFields(source.getFields());
    return target;

  }
}
