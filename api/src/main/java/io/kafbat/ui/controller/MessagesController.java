package io.kafbat.ui.controller;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_DELETE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_PRODUCE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_READ;

import io.kafbat.ui.api.MessagesApi;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.MessageFilterIdDTO;
import io.kafbat.ui.model.MessageFilterRegistrationDTO;
import io.kafbat.ui.model.MessageFilterTypeDTO;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.SeekDirectionDTO;
import io.kafbat.ui.model.SeekTypeDTO;
import io.kafbat.ui.model.SerdeUsageDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionResultDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.model.TopicSerdeSuggestionDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.AuditAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.service.DeserializationService;
import io.kafbat.ui.service.MessagesService;
import io.kafbat.ui.service.mcp.McpTool;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessagesController extends AbstractController implements MessagesApi, McpTool {

  private final MessagesService messagesService;
  private final DeserializationService deserializationService;

  @Override
  public Mono<ResponseEntity<Void>> deleteTopicMessages(
      String clusterName, String topicName, @Valid List<Integer> partitions,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, MESSAGES_DELETE)
        .build();

    return validateAccess(context).<ResponseEntity<Void>>then(
        messagesService.deleteTopicMessages(
            getCluster(clusterName),
            topicName,
            Optional.ofNullable(partitions).orElse(List.of())
        ).thenReturn(ResponseEntity.ok().build())
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<SmartFilterTestExecutionResultDTO>> executeSmartFilterTest(
      Mono<SmartFilterTestExecutionDTO> smartFilterTestExecutionDto, ServerWebExchange exchange) {
    return smartFilterTestExecutionDto
        .map(MessagesService::execSmartFilterTest)
        .map(ResponseEntity::ok);
  }

  @Deprecated(forRemoval = true, since = "1.1.0")
  @Override
  public Mono<ResponseEntity<Flux<TopicMessageEventDTO>>> getTopicMessages(String clusterName,
                                                                           String topicName,
                                                                           SeekTypeDTO seekType,
                                                                           List<String> seekTo,
                                                                           Integer limit,
                                                                           String q,
                                                                           MessageFilterTypeDTO filterQueryType,
                                                                           SeekDirectionDTO seekDirection,
                                                                           String keySerde,
                                                                           String valueSerde,
                                                                           ServerWebExchange exchange) {
    throw new ValidationException("Not supported");
  }


  @Override
  public Mono<ResponseEntity<Flux<TopicMessageEventDTO>>> getTopicMessagesV2(String clusterName, String topicName,
                                                                             PollingModeDTO mode,
                                                                             List<Integer> partitions,
                                                                             Integer limit,
                                                                             String stringFilter,
                                                                             String smartFilterId,
                                                                             Long offset,
                                                                             Long timestamp,
                                                                             String keySerde,
                                                                             String valueSerde,
                                                                             String cursor,
                                                                             ServerWebExchange exchange) {
    var contextBuilder = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getTopicMessages");

    if (auditService.isAuditTopic(getCluster(clusterName), topicName)) {
      contextBuilder.auditActions(AuditAction.VIEW);
    } else {
      contextBuilder.topicActions(topicName, MESSAGES_READ);
    }

    var accessContext = contextBuilder.build();

    Flux<TopicMessageEventDTO> messagesFlux;
    if (cursor != null) {
      messagesFlux = messagesService.loadMessages(getCluster(clusterName), topicName, cursor);
    } else {
      var pollingMode = mode == null ? PollingModeDTO.LATEST : mode;
      messagesFlux = messagesService.loadMessages(
          getCluster(clusterName),
          topicName,
          ConsumerPosition.create(pollingMode, checkNotNull(topicName), partitions, timestamp, offset),
          stringFilter,
          smartFilterId,
          limit,
          keySerde,
          valueSerde
      );
    }
    return accessControlService.validateAccess(accessContext)
        .then(Mono.just(ResponseEntity.ok(messagesFlux)))
        .doOnEach(sig -> auditService.audit(accessContext, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> sendTopicMessages(
      String clusterName, String topicName, @Valid Mono<CreateTopicMessageDTO> createTopicMessage,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, MESSAGES_PRODUCE)
        .operationName("sendTopicMessages")
        .build();

    return validateAccess(context).then(
        createTopicMessage.flatMap(msg ->
            messagesService.sendMessage(getCluster(clusterName), topicName, msg)
        ).map(m -> new ResponseEntity<Void>(HttpStatus.OK))
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<TopicSerdeSuggestionDTO>> getSerdes(String clusterName,
                                                                 String topicName,
                                                                 SerdeUsageDTO use,
                                                                 ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, TopicAction.VIEW)
        .operationName("getSerdes")
        .build();

    TopicSerdeSuggestionDTO dto = new TopicSerdeSuggestionDTO()
        .key(use == SerdeUsageDTO.SERIALIZE
            ? deserializationService.getSerdesForSerialize(getCluster(clusterName), topicName, Serde.Target.KEY)
            : deserializationService.getSerdesForDeserialize(getCluster(clusterName), topicName, Serde.Target.KEY))
        .value(use == SerdeUsageDTO.SERIALIZE
            ? deserializationService.getSerdesForSerialize(getCluster(clusterName), topicName, Serde.Target.VALUE)
            : deserializationService.getSerdesForDeserialize(getCluster(clusterName), topicName, Serde.Target.VALUE));

    return validateAccess(context).then(
        Mono.just(dto)
            .subscribeOn(Schedulers.boundedElastic())
            .map(ResponseEntity::ok)
    );
  }

  @Override
  public Mono<ResponseEntity<MessageFilterIdDTO>> registerFilter(String clusterName,
                                                                 String topicName,
                                                                 Mono<MessageFilterRegistrationDTO> registration,
                                                                 ServerWebExchange exchange) {


    final Mono<Void> validateAccess = accessControlService.validateAccess(AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, MESSAGES_READ)
        .build());
    return validateAccess.then(registration)
        .map(reg -> messagesService.registerMessageFilter(reg.getFilterCode()))
        .map(id -> ResponseEntity.ok(new MessageFilterIdDTO().id(id)));
  }
}
