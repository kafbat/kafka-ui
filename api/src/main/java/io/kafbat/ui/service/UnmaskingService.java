package io.kafbat.ui.service;

import io.kafbat.ui.client.ServiceNowClient;
import io.kafbat.ui.config.auth.AuthenticatedUser;
import io.kafbat.ui.exception.TopicNotFoundException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.mapper.DynamicConfigMapper;
import io.kafbat.ui.model.*;
import io.kafbat.ui.model.rbac.DynamoRbacEntity;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.sainsburys.ServiceNowCreate;
import io.kafbat.ui.model.sainsburys.ServiceNowRequestConfig;
import io.kafbat.ui.repository.DynamoRbacEntityRepository;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.ApplicationRestarter;
import io.kafbat.ui.util.DynamicConfigOperations;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UnmaskingService {

  public static final String KAFKA_CLUSTER_SERVICENOW_DESCRIPTION = "<kafka_cluster>";
  public static final String KAFKA_TOPIC_SERVICENOW_DESCRIPTION = "<kafka_topic>";
  public static final String JUSTIFICATION_SERVICENOW_DESCRIPTION = "<justification>";
  public static final String RBAC_UNMASK_USER_ROLE_S_S_S_UNMASK = "%s_%s_%s_unmask";
  private final AdminClientService adminClientService;
  private final ServiceNowClient serviceNowClient;
  private final DynamicConfigOperations dynamicConfigOperations;
  private final DynamicConfigMapper configMapper;
  private final ApplicationRestarter restarter;
  private final ServiceNowRequestConfig serviceNowRequestConfig;
  private final DynamoRbacEntityRepository dynamoRbacEntityRepository;

  @Value("${kit.masking.rule.time-to-live: 3600000}")
  private Long timeToLive;

  @Value("${kit.masking.date.format: yyyy-MM-dd HH:mm:ss.SSS }")
  private String dateFormat;

  @Value("${kit.masking.subject.type: user }")
  private String subjectType;

  @Value("${kit.masking.cluster.unmasked-prefix:unmasked-}")
  private String unmaskedClusterPrefix;

  public UnmaskingService(AdminClientService adminClientService, ServiceNowClient serviceNowClient,
                          DynamicConfigOperations dynamicConfigOperations, DynamicConfigMapper configMapper,
                          ApplicationRestarter restarter, ServiceNowRequestConfig serviceNowRequestConfig,
                          DynamoRbacEntityRepository dynamoRbacEntityRepository) {
    this.adminClientService = adminClientService;
    this.serviceNowClient = serviceNowClient;
    this.dynamicConfigOperations = dynamicConfigOperations;
    this.configMapper = configMapper;
    this.restarter = restarter;
    this.serviceNowRequestConfig = serviceNowRequestConfig;
    this.dynamoRbacEntityRepository = dynamoRbacEntityRepository;
  }

  private Mono<TopicDescription> withExistingTopic(KafkaCluster cluster, String topicName) {
    return adminClientService.get(cluster)
        .flatMap(client -> client.describeTopic(topicName))
        .switchIfEmpty(Mono.error(new TopicNotFoundException()));
  }

  public Mono<RecordMetadata> decrypt(KafkaCluster cluster, String topic,
                                      UnmaskRequestDTO msg, String principal) {
    log.info("Unmasking Request: {}", msg.getJustification());
    return withExistingTopic(cluster, topic)
        .publishOn(Schedulers.boundedElastic())
        .flatMap(desc -> decryptImpl(cluster, desc, msg, principal));
  }

  private Mono<RecordMetadata> decryptImpl(KafkaCluster cluster,
                                           TopicDescription topicDescription,
                                           UnmaskRequestDTO msg, String principal) {
    if (msg.getJustification() == null) {
      return Mono.error(new ValidationException("No justification provided for request"));
    }

    try {
      boolean isAuditCreated = logServiceNowTicket(cluster.getName(),
                                topicDescription.name(),
                                msg.getJustification(),
          principal);
      if(isAuditCreated) {
        ApplicationConfigPropertiesDTO config = configMapper.toDto(dynamicConfigOperations.getCurrentProperties());
        AtomicBoolean isRoleAssigned = new AtomicBoolean(false);
        updateRbacConfig(cluster.getName(), topicDescription.name(), principal, config, isRoleAssigned);
        log.info("Persist cluster config change");
        config.getRbac().getRoles().stream().forEach(r->{
          log.info("RBAC Role: {}", r.getName());
        });
        if(isRoleAssigned.get()){
          var newConfig = configMapper.fromDto(config);
          dynamicConfigOperations.persist(newConfig);
          restarter.requestRestart();
        }
      }
      return Mono.empty();
    } catch (Throwable e) {
      return Mono.error(e);
    }
  }


  private boolean logServiceNowTicket(String cluster, String topic, String justification, String username){
    try{
      ServiceNowCreate payload = buildServiceNowCreatePayload(cluster, topic, justification, username);
      ResponseEntity<Object> response = serviceNowClient.createAuditTicket( payload);
      return response != null && response.getStatusCode().is2xxSuccessful();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void updateRbacConfig(String cluster, String topic, String principal, ApplicationConfigPropertiesDTO config,
                                AtomicBoolean isRoleAssigned){
    String unmaskPrincipalRole = String.format(RBAC_UNMASK_USER_ROLE_S_S_S_UNMASK, cluster, topic, principal);
    List<@Valid ApplicationConfigPropertiesRbacRolesInnerDTO> configPropertiesRbacRolesInnerDTOList = config.getRbac().getRoles();

    boolean isUnmaskRoleNotExist = configPropertiesRbacRolesInnerDTOList.stream()
        .filter(r->r.getName().contains(unmaskPrincipalRole))
        .toList()
        .isEmpty();

    if(isUnmaskRoleNotExist){
      ApplicationConfigPropertiesRbacRolesInnerDTO rbacRolesInnerDTO = new ApplicationConfigPropertiesRbacRolesInnerDTO();
      rbacRolesInnerDTO.setName(unmaskPrincipalRole);
      rbacRolesInnerDTO.setClusters(Arrays.asList(unmaskedClusterPrefix + cluster));

      ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO rbacRolesInnerSubjectsInnerDTO =
          getApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO(cluster, principal, config);

      rbacRolesInnerDTO.setSubjects(Arrays.asList(rbacRolesInnerSubjectsInnerDTO));

      ApplicationConfigPropertiesRbacRolesInnerPermissionsInnerDTO clusterPermissionsInnerDTO = new ApplicationConfigPropertiesRbacRolesInnerPermissionsInnerDTO();
      clusterPermissionsInnerDTO.setResource(ResourceTypeDTO.CLUSTERCONFIG);
      clusterPermissionsInnerDTO.setActions(Arrays.asList(ActionDTO.VIEW));

      rbacRolesInnerDTO.addPermissionsItem(clusterPermissionsInnerDTO);

      ApplicationConfigPropertiesRbacRolesInnerPermissionsInnerDTO topicPermissionsInnerDTO = new ApplicationConfigPropertiesRbacRolesInnerPermissionsInnerDTO();
      topicPermissionsInnerDTO.setResource(ResourceTypeDTO.TOPIC);
      topicPermissionsInnerDTO.setActions(Arrays.asList(ActionDTO.VIEW, ActionDTO.MESSAGES_READ));
      topicPermissionsInnerDTO.setValue(topic);

      rbacRolesInnerDTO.addPermissionsItem(topicPermissionsInnerDTO);
      config.getRbac().addRolesItem(rbacRolesInnerDTO);
      isRoleAssigned.set(true);
      createDynamoRbac(mapperFromRbacRoleDto(rbacRolesInnerDTO));
    }else{
      throw new ValidationException("Data unmask role already assigned");
    }
  }

  private @NonNull ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO getApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO(String cluster,
      String principal, ApplicationConfigPropertiesDTO config) {
    ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO rbacRolesInnerSubjectsInnerDTO = new ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO();

    assingPrincipalAuthProvider(cluster, principal, config, rbacRolesInnerSubjectsInnerDTO);

    rbacRolesInnerSubjectsInnerDTO.setValue(principal);
    rbacRolesInnerSubjectsInnerDTO.setType(subjectType);

    Calendar calendar = Calendar.getInstance();
    Date currentDate = calendar.getTime();

    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    String createdTime = sdf.format(currentDate);

    rbacRolesInnerSubjectsInnerDTO.setCreatedTime(createdTime);

    calendar.add(Calendar.MILLISECOND, timeToLive.intValue());
    Date expiryTime = calendar.getTime();
    String expiryTimeStr = sdf.format(expiryTime);

    rbacRolesInnerSubjectsInnerDTO.setExpiryTime(expiryTimeStr);
    return rbacRolesInnerSubjectsInnerDTO;
  }

  private ServiceNowCreate buildServiceNowCreatePayload(String cluster, String topic, String justification, String username){

    String description = serviceNowRequestConfig.getU_description().replace(KAFKA_CLUSTER_SERVICENOW_DESCRIPTION, cluster + "\n");
    description = description.replace(KAFKA_TOPIC_SERVICENOW_DESCRIPTION, topic + "\n");
    description = description.replace(JUSTIFICATION_SERVICENOW_DESCRIPTION, justification);

    return ServiceNowCreate.builder()
        .u_assigned_to(serviceNowRequestConfig.getU_assigned_to())
        .u_assignment_group(serviceNowRequestConfig.getU_assignment_group())
        .u_business_service(serviceNowRequestConfig.getU_business_service())
        .u_caller_id(username)
        .u_category(serviceNowRequestConfig.getU_category())
        .u_subcategory(serviceNowRequestConfig.getU_subcategory())
        .u_cmdb_ci(serviceNowRequestConfig.getU_cmdb_ci())
        .u_comments(serviceNowRequestConfig.getU_comments())
        .u_description(description)
        .u_impact(serviceNowRequestConfig.getU_impact())
        .u_urgency(serviceNowRequestConfig.getU_urgency())
        .u_impacted_parties(serviceNowRequestConfig.getU_impacted_parties())
        .u_location_not_found(serviceNowRequestConfig.getU_location_not_found())
        .u_undefined_location(serviceNowRequestConfig.getU_undefined_location())
        .u_short_description(serviceNowRequestConfig.getU_short_description())
        .u_state(serviceNowRequestConfig.getU_state())
        .u_work_notes(serviceNowRequestConfig.getU_work_notes())
        .build();
  }

  private static void assingPrincipalAuthProvider(String cluster, String principal, ApplicationConfigPropertiesDTO config,
                                ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO rbacRolesInnerSubjectsInnerDTO) {
    config.getRbac().getRoles().stream()
        .filter(r->r.getClusters().contains(cluster))
            .forEach(role->{
              ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO
                  principalRole = role.getSubjects().stream()
                  .filter(s -> s.getValue().equalsIgnoreCase(principal))
                  .findFirst()
                  .orElse(null);
              if (principalRole != null){
                rbacRolesInnerSubjectsInnerDTO.setProvider(principalRole.getProvider());
              }
            });
  }

  private void createDynamoRbac(DynamoRbacEntity rbac){
    try{
      dynamoRbacEntityRepository.save(rbac);
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  private DynamoRbacEntity mapperFromRbacRoleDto(ApplicationConfigPropertiesRbacRolesInnerDTO source){
    return DynamoRbacEntity.builder()
        .name(source.getName())
        .clusters(source.getClusters())
        .subjects(source.getSubjects().stream().map(this::mapperFromSubjectsDto).toList())
        .permissions(source.getPermissions().stream().map(this::mapperFromPermissionsDto).toList())
        .build();
  }

  private Subject mapperFromSubjectsDto(ApplicationConfigPropertiesRbacRolesInnerSubjectsInnerDTO source){
    Subject target = new Subject();
    target.setType(source.getType());
    target.setValue(source.getValue());
//    target.setProvider(source.getProvider());
//    target.setRegex(source.getRegex());
    target.setCreatedTime(source.getCreatedTime());
    target.setExpiryTime(source.getExpiryTime());

    return target;
  }

  private Permission mapperFromPermissionsDto(ApplicationConfigPropertiesRbacRolesInnerPermissionsInnerDTO source){
    Permission target = new Permission();
    target.setValue(source.getValue());
    target.setActions(source.getActions().stream().map(ActionDTO::getValue).toList());
    target.setResource(source.getResource().getValue());
    return target;
  }

}
