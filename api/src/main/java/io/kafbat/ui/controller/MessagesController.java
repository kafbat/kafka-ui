package io.kafbat.ui.controller;

import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_DELETE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_PRODUCE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.MESSAGES_READ;
import static java.util.stream.Collectors.toMap;

import io.kafbat.ui.api.MessagesApi;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.MessageFilterTypeDTO;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.TopicPartition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessagesController extends AbstractController implements MessagesApi {

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
    var contextBuilder = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, MESSAGES_READ)
        .operationName("getTopicMessages");

    if (auditService.isAuditTopic(getCluster(clusterName), topicName)) {
      contextBuilder.auditActions(AuditAction.VIEW);
    }

    seekType = seekType != null ? seekType : SeekTypeDTO.BEGINNING;
    seekDirection = seekDirection != null ? seekDirection : SeekDirectionDTO.FORWARD;
    filterQueryType = filterQueryType != null ? filterQueryType : MessageFilterTypeDTO.STRING_CONTAINS;

    var positions = new ConsumerPosition(
        seekType,
        topicName,
        parseSeekTo(topicName, seekType, seekTo)
    );
    Mono<ResponseEntity<Flux<TopicMessageEventDTO>>> job = Mono.just(
        ResponseEntity.ok(
            messagesService.loadMessages(
                getCluster(clusterName), topicName, positions, q, filterQueryType,
                limit, seekDirection, keySerde, valueSerde)
        )
    );

    var context = contextBuilder.build();
    return validateAccess(context)
        .then(job)
        .doOnEach(sig -> audit(context, sig));
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
            messagesService.sendMessage(getCluster(clusterName), topicName, msg).then()
        ).map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  /**
   * The format is [partition]::[offset] for specifying offsets
   * or [partition]::[timestamp in millis] for specifying timestamps.
   */
  @Nullable
  private Map<TopicPartition, Long> parseSeekTo(String topic, SeekTypeDTO seekType, List<String> seekTo) {
    if (seekTo == null || seekTo.isEmpty()) {
      if (seekType == SeekTypeDTO.LATEST || seekType == SeekTypeDTO.BEGINNING) {
        return null;
      }
      throw new ValidationException("seekTo should be set if seekType is " + seekType);
    }
    return seekTo.stream()
        .map(p -> {
          String[] split = p.split("::");
          if (split.length != 2) {
            throw new IllegalArgumentException(
                "Wrong seekTo argument format. See API docs for details");
          }

          return Pair.of(
              new TopicPartition(topic, Integer.parseInt(split[0])),
              Long.parseLong(split[1])
          );
        })
        .collect(toMap(Pair::getKey, Pair::getValue));
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




}
