package io.kafbat.ui.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.RateLimiter;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.emitter.BackwardEmitter;
import io.kafbat.ui.emitter.Cursor;
import io.kafbat.ui.emitter.ForwardEmitter;
import io.kafbat.ui.emitter.MessageFilters;
import io.kafbat.ui.emitter.TailingEmitter;
import io.kafbat.ui.exception.TopicNotFoundException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionResultDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import io.kafbat.ui.serdes.ProducerRecordCreator;
import io.kafbat.ui.util.KafkaClientSslPropertiesUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class MessagesService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final long SALT_FOR_HASHING = ThreadLocalRandom.current().nextLong();

  private static final int DEFAULT_MAX_PAGE_SIZE = 500;
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int DEFAULT_UPLOAD_MESSAGE_LIMIT = 1_000;
  private static final int MAX_UPLOAD_MESSAGE_LIMIT = 10_000;

  // limiting UI messages rate to 20/sec in tailing mode
  private static final int TAILING_UI_MESSAGE_THROTTLE_RATE = 20;
  private static final String INVALID_ZIP_ENTRY_CHARS = "[\\\\/:*?\"<>|\\p{Cntrl}]";

  private final AdminClientService adminClientService;
  private final DeserializationService deserializationService;
  private final ConsumerGroupService consumerGroupService;
  private final int maxPageSize;
  private final int defaultPageSize;

  private final Cache<String, Predicate<TopicMessageDTO>> registeredFilters = CacheBuilder.newBuilder()
      .maximumSize(PollingCursorsStorage.MAX_SIZE)
      .build();

  private final PollingCursorsStorage cursorsStorage = new PollingCursorsStorage();

  public MessagesService(AdminClientService adminClientService,
                         DeserializationService deserializationService,
                         ConsumerGroupService consumerGroupService,
                         ClustersProperties properties) {
    this.adminClientService = adminClientService;
    this.deserializationService = deserializationService;
    this.consumerGroupService = consumerGroupService;

    var pollingProps = Optional.ofNullable(properties.getPolling())
        .orElseGet(ClustersProperties.PollingProperties::new);
    this.maxPageSize = Optional.ofNullable(pollingProps.getMaxPageSize())
        .orElse(DEFAULT_MAX_PAGE_SIZE);
    this.defaultPageSize = Optional.ofNullable(pollingProps.getDefaultPageSize())
        .orElse(DEFAULT_PAGE_SIZE);
  }

  public enum DownloadFormat {
    TEXT,
    JSON,
    VALUE_ONLY;

    public static DownloadFormat fromRequest(@Nullable String value) {
      if (value == null || value.isBlank()) {
        return TEXT;
      }
      try {
        return DownloadFormat.valueOf(value.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new ValidationException("Unsupported download format: " + value);
      }
    }
  }

  public enum UploadParseMode {
    FILE_PER_MESSAGE,
    TEXT_LINES,
    NDJSON,
    JSON_ARRAY;

    public static UploadParseMode fromRequest(@Nullable String value) {
      if (value == null || value.isBlank()) {
        return FILE_PER_MESSAGE;
      }
      try {
        return UploadParseMode.valueOf(value.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new ValidationException("Unsupported upload parse mode: " + value);
      }
    }
  }

  public enum UploadPartitionStrategy {
    ANY,
    SELECTED,
    RANDOM,
    EVEN;

    public static UploadPartitionStrategy fromRequest(@Nullable String value) {
      if (value == null || value.isBlank()) {
        return ANY;
      }
      try {
        return UploadPartitionStrategy.valueOf(value.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new ValidationException("Unsupported upload partition strategy: " + value);
      }
    }
  }

  public enum UploadKeyMode {
    NONE,
    FILE_NAME,
    ENTRY_NAME;

    public static UploadKeyMode fromRequest(@Nullable String value) {
      if (value == null || value.isBlank()) {
        return NONE;
      }
      try {
        return UploadKeyMode.valueOf(value.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new ValidationException("Unsupported upload key mode: " + value);
      }
    }
  }

  public record UploadSourceFile(String fileName, byte[] content) {
  }

  public record UploadMessagesOptions(UploadParseMode parseMode,
                                      UploadPartitionStrategy partitionStrategy,
                                      UploadKeyMode keyMode,
                                      @Nullable Integer selectedPartition,
                                      List<Integer> targetPartitions,
                                      @Nullable String keySerde,
                                      @Nullable String valueSerde,
                                      @Nullable String headersJson,
                                      boolean includeMetadataHeaders,
                                      boolean dryRun,
                                      @Nullable Integer messageLimit) {
  }

  public record UploadMessagesFileResult(String fileName,
                                         int extractedEntries,
                                         int parsedMessages) {
  }

  public record UploadMessagePreview(String sourceFile,
                                     String entryName,
                                     @Nullable Integer partition,
                                     @Nullable String key,
                                     int valueBytes,
                                     String valuePreview) {
  }

  public record UploadMessagesResult(boolean dryRun,
                                     int filesReceived,
                                     int entriesRead,
                                     int messagesParsed,
                                     int messagesProduced,
                                     int failures,
                                     List<UploadMessagesFileResult> files,
                                     List<UploadMessagePreview> previews,
                                     List<String> errors) {
  }

  private record UploadCandidate(String sourceFile,
                                 String entryName,
                                 @Nullable String key,
                                 String value,
                                 int sourceIndex) {
  }

  private Mono<TopicDescription> withExistingTopic(KafkaCluster cluster, String topicName) {
    return adminClientService.get(cluster)
        .flatMap(client -> client.describeTopic(topicName))
        .switchIfEmpty(Mono.error(new TopicNotFoundException()));
  }

  public static SmartFilterTestExecutionResultDTO execSmartFilterTest(SmartFilterTestExecutionDTO execData) {
    Predicate<TopicMessageDTO> predicate;
    try {
      predicate = MessageFilters.celScriptFilter(execData.getFilterCode());
    } catch (Exception e) {
      log.info("Smart filter '{}' compilation error", execData.getFilterCode(), e);
      return new SmartFilterTestExecutionResultDTO()
          .error("Compilation error : " + e.getMessage());
    }
    try {
      var result = predicate.test(
          new TopicMessageDTO()
              .key(execData.getKey())
              .value(execData.getValue())
              .headers(execData.getHeaders())
              .offset(execData.getOffset())
              .partition(execData.getPartition())
              .timestamp(
                  Optional.ofNullable(execData.getTimestampMs())
                      .map(ts -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC))
                      .orElse(null))
      );
      return new SmartFilterTestExecutionResultDTO()
          .result(result);
    } catch (Exception e) {
      log.info("Smart filter {} execution error", execData, e);
      return new SmartFilterTestExecutionResultDTO()
          .error("Execution error : " + e.getMessage());
    }
  }

  public Mono<Void> deleteTopicMessages(KafkaCluster cluster, String topicName,
                                        List<Integer> partitionsToInclude) {
    return withExistingTopic(cluster, topicName)
        .flatMap(td ->
            offsetsForDeletion(cluster, topicName, partitionsToInclude)
                .flatMap(offsets ->
                    adminClientService.get(cluster).flatMap(ac -> ac.deleteRecords(offsets))));
  }

  private Mono<Map<TopicPartition, Long>> offsetsForDeletion(KafkaCluster cluster, String topicName,
                                                             List<Integer> partitionsToInclude) {
    return adminClientService.get(cluster).flatMap(ac ->
        ac.listTopicOffsets(topicName, OffsetSpec.earliest(), true)
            .zipWith(ac.listTopicOffsets(topicName, OffsetSpec.latest(), true),
                (start, end) ->
                    end.entrySet().stream()
                        .filter(e -> partitionsToInclude.isEmpty()
                            || partitionsToInclude.contains(e.getKey().partition()))
                        // we only need non-empty partitions (where start offset != end offset)
                        .filter(entry -> !entry.getValue().equals(start.get(entry.getKey())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    );
  }

  public Mono<RecordMetadata> sendMessage(KafkaCluster cluster, String topic,
                                          CreateTopicMessageDTO msg) {
    return withExistingTopic(cluster, topic)
        .publishOn(Schedulers.boundedElastic())
        .flatMap(desc -> sendMessageImpl(cluster, desc, msg));
  }

  private Mono<RecordMetadata> sendMessageImpl(KafkaCluster cluster,
                                               TopicDescription topicDescription,
                                               CreateTopicMessageDTO msg) {
    if (msg.getPartition() != null
        && msg.getPartition() > topicDescription.partitions().size() - 1) {
      return Mono.error(new ValidationException("Invalid partition"));
    }
    ProducerRecordCreator producerRecordCreator =
        deserializationService.producerRecordCreator(
            cluster,
            topicDescription.name(),
            msg.getKeySerde().get(),
            msg.getValueSerde().get(),
            msg.getKeySerdeProperties(),
            msg.getValueSerdeProperties()
        );

    try (KafkaProducer<byte[], byte[]> producer = createProducer(cluster, Map.of())) {
      ProducerRecord<byte[], byte[]> producerRecord = producerRecordCreator.create(
          topicDescription.name(),
          msg.getPartition(),
          msg.getKey().orElse(null),
          msg.getValue().orElse(null),
          msg.getHeaders()
      );
      CompletableFuture<RecordMetadata> cf = new CompletableFuture<>();
      producer.send(producerRecord, (metadata, exception) -> {
        if (exception != null) {
          cf.completeExceptionally(exception);
        } else {
          cf.complete(metadata);
        }
      });
      return Mono.fromFuture(cf);
    } catch (Throwable e) {
      return Mono.error(e);
    }
  }

  public static KafkaProducer<byte[], byte[]> createProducer(KafkaCluster cluster,
                                                             Map<String, Object> additionalProps) {
    return createProducer(cluster.getOriginalProperties(), additionalProps);
  }

  public static KafkaProducer<byte[], byte[]> createProducer(ClustersProperties.Cluster cluster,
                                                             Map<String, Object> additionalProps) {
    Properties properties = new Properties();
    KafkaClientSslPropertiesUtil.addKafkaSslProperties(cluster.getSsl(), properties);
    properties.putAll(cluster.getProperties());
    properties.putAll(cluster.getProducerProperties());
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    properties.putAll(additionalProps);
    return new KafkaProducer<>(properties);
  }

  public Flux<TopicMessageEventDTO> loadMessages(KafkaCluster cluster,
                                                 String topic,
                                                 ConsumerPosition consumerPosition,
                                                 @Nullable String containsStringFilter,
                                                 @Nullable String filterId,
                                                 @Nullable Integer limit,
                                                 @Nullable String keySerde,
                                                 @Nullable String valueSerde) {
    return loadMessages(
        cluster,
        topic,
        deserializationService.deserializerFor(cluster, topic, keySerde, valueSerde),
        consumerPosition,
        getMsgFilter(containsStringFilter, filterId),
        fixPageSize(limit)
    );
  }

  public Flux<TopicMessageEventDTO> loadMessages(KafkaCluster cluster, String topic, String cursorId) {
    Cursor cursor = cursorsStorage.getCursor(cursorId)
        .orElseThrow(() -> new ValidationException("Next page cursor not found. Maybe it was evicted from cache."));
    return loadMessages(
        cluster,
        topic,
        cursor.deserializer(),
        cursor.consumerPosition(),
        cursor.filter(),
        fixPageSize(cursor.limit())
    );
  }

  private Flux<TopicMessageEventDTO> loadMessages(KafkaCluster cluster,
                                                  String topic,
                                                  ConsumerRecordDeserializer deserializer,
                                                  ConsumerPosition consumerPosition,
                                                  Predicate<TopicMessageDTO> filter,
                                                  int limit) {
    return withExistingTopic(cluster, topic)
        .flux()
        .publishOn(Schedulers.boundedElastic())
        .flatMap(td -> loadMessagesImpl(cluster, deserializer, consumerPosition, filter, limit));
  }

  public Mono<byte[]> downloadLastMessagesAsZip(KafkaCluster cluster,
                                                String topic,
                                                int limit,
                                                List<Integer> partitions,
                                                @Nullable String containsStringFilter,
                                                @Nullable String smartFilterId,
                                                @Nullable String keySerde,
                                                @Nullable String valueSerde) {
    return downloadMessagesAsZip(
        cluster,
        topic,
        limit,
        partitions,
        containsStringFilter,
        smartFilterId,
        keySerde,
        valueSerde,
        PollingModeDTO.LATEST,
        null,
        null,
        null,
        DownloadFormat.TEXT
    );
  }

  public Mono<byte[]> downloadMessagesAsZip(KafkaCluster cluster,
                                            String topic,
                                            int limit,
                                            List<Integer> partitions,
                                            @Nullable String containsStringFilter,
                                            @Nullable String smartFilterId,
                                            @Nullable String keySerde,
                                            @Nullable String valueSerde,
                                            PollingModeDTO pollingMode,
                                            @Nullable Long offset,
                                            @Nullable Long timestamp,
                                            @Nullable Long timestampTo,
                                            DownloadFormat format) {
    if (limit < 1 || limit > maxPageSize) {
      return Mono.error(new ValidationException(
          "Download limit must be between 1 and " + maxPageSize));
    }

    return loadMessages(
        cluster,
        topic,
        ConsumerPosition.create(pollingMode, topic, partitions, timestamp, offset),
        containsStringFilter,
        smartFilterId,
        limit,
        keySerde,
        valueSerde
    )
        .filter(event -> event.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE)
        .map(TopicMessageEventDTO::getMessage)
        .filter(message -> matchesTimestampUpperBound(message, timestampTo))
        .collectList()
        .map(messages -> createMessagesZip(topic, messages, format));
  }

  public Mono<UploadMessagesResult> uploadMessages(KafkaCluster cluster,
                                                   String topic,
                                                   List<UploadSourceFile> files,
                                                   UploadMessagesOptions options) {
    if (files.isEmpty()) {
      return Mono.error(new ValidationException("At least one upload file is required"));
    }
    validateUploadSerdes(options);

    return withExistingTopic(cluster, topic)
        .publishOn(Schedulers.boundedElastic())
        .map(topicDescription -> uploadMessagesImpl(cluster, topicDescription, files, options));
  }

  public int resolveDownloadLimit(@Nullable Integer limit) {
    int resolvedLimit = Optional.ofNullable(limit).orElse(defaultPageSize);
    if (resolvedLimit < 1 || resolvedLimit > maxPageSize) {
      throw new ValidationException("Download limit must be between 1 and " + maxPageSize);
    }
    return resolvedLimit;
  }

  public String downloadFileName(String topic, int limit) {
    return safeZipName(topic) + "-last-" + limit + "-messages.zip";
  }

  public String downloadFileName(String topic, int limit, PollingModeDTO mode, DownloadFormat format) {
    String suffix = format == DownloadFormat.JSON ? "json" : "txt";
    return safeZipName(topic) + "-" + mode.name().toLowerCase(Locale.ROOT) + "-" + limit
        + "-" + suffix + "-messages.zip";
  }

  private Flux<TopicMessageEventDTO> loadMessagesImpl(KafkaCluster cluster,
                                                      ConsumerRecordDeserializer deserializer,
                                                      ConsumerPosition consumerPosition,
                                                      Predicate<TopicMessageDTO> filter,
                                                      int limit) {
    var emitter = switch (consumerPosition.pollingMode()) {
      case TO_OFFSET, TO_TIMESTAMP, LATEST -> new BackwardEmitter(
          () -> consumerGroupService.createConsumer(cluster,
              Map.of(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, limit)),
          consumerPosition,
          limit,
          deserializer,
          filter,
          cluster.getPollingSettings(),
          cursorsStorage.createNewCursor(deserializer, consumerPosition, filter, limit)
      );
      case FROM_OFFSET, FROM_TIMESTAMP, EARLIEST -> new ForwardEmitter(
          () -> consumerGroupService.createConsumer(cluster,
              Map.of(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, limit)),
          consumerPosition,
          limit,
          deserializer,
          filter,
          cluster.getPollingSettings(),
          cursorsStorage.createNewCursor(deserializer, consumerPosition, filter, limit)
      );
      case TAILING -> new TailingEmitter(
          () -> consumerGroupService.createConsumer(cluster),
          consumerPosition,
          deserializer,
          filter,
          cluster.getPollingSettings()
      );
    };
    return Flux.create(emitter)
        .map(throttleUiPublish(consumerPosition.pollingMode()));
  }

  private Predicate<TopicMessageDTO> getMsgFilter(@Nullable String containsStrFilter,
                                                  @Nullable String smartFilterId) {
    Predicate<TopicMessageDTO> messageFilter = MessageFilters.noop();
    if (containsStrFilter != null) {
      messageFilter = messageFilter.and(MessageFilters.containsStringFilter(containsStrFilter));
    }
    if (smartFilterId != null) {
      var registered = registeredFilters.getIfPresent(smartFilterId);
      if (registered == null) {
        throw new ValidationException("No filter was registered with id " + smartFilterId);
      }
      messageFilter = messageFilter.and(registered);
    }
    return messageFilter;
  }

  private <T> UnaryOperator<T> throttleUiPublish(PollingModeDTO pollingMode) {
    if (pollingMode == PollingModeDTO.TAILING) {
      RateLimiter rateLimiter = RateLimiter.create(TAILING_UI_MESSAGE_THROTTLE_RATE);
      return m -> {
        rateLimiter.acquire(1);
        return m;
      };
    }
    // there is no need to throttle UI production rate for non-tailing modes, since max number of produced
    // messages is limited for them (with page size)
    return UnaryOperator.identity();
  }

  private int fixPageSize(@Nullable Integer pageSize) {
    return Optional.ofNullable(pageSize)
        .filter(ps -> ps > 0 && ps <= maxPageSize)
        .orElse(defaultPageSize);
  }

  public String registerMessageFilter(String celCode) {
    String saltedCode = celCode + SALT_FOR_HASHING;
    String filterId = Hashing.sha256()
        .hashString(saltedCode, Charsets.UTF_8)
        .toString()
        .substring(0, 8);
    if (registeredFilters.getIfPresent(filterId) == null) {
      registeredFilters.put(filterId, MessageFilters.celScriptFilter(celCode));
    }
    return filterId;
  }

  private boolean matchesTimestampUpperBound(TopicMessageDTO message, @Nullable Long timestampTo) {
    if (timestampTo == null || message.getTimestamp() == null) {
      return true;
    }
    return !message.getTimestamp().toInstant().isAfter(Instant.ofEpochMilli(timestampTo));
  }

  private byte[] createMessagesZip(String topic, List<TopicMessageDTO> messages, DownloadFormat format) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {

      if (messages.isEmpty()) {
        zipOutputStream.putNextEntry(new ZipEntry("README.txt"));
        zipOutputStream.write(("No messages were found for topic " + topic + ".")
            .getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
      }

      String safeTopic = safeZipName(topic);
      for (TopicMessageDTO message : messages) {
        zipOutputStream.putNextEntry(new ZipEntry(messageFileName(message, safeTopic, format)));
        zipOutputStream.write(messageContent(topic, message, format).getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
      }

      zipOutputStream.finish();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create Kafka messages ZIP archive", e);
    }
  }

  private UploadMessagesResult uploadMessagesImpl(KafkaCluster cluster,
                                                  TopicDescription topicDescription,
                                                  List<UploadSourceFile> files,
                                                  UploadMessagesOptions options) {
    ParsedUpload parsedUpload = parseUploadFiles(files, options);
    int messageLimit = resolveUploadMessageLimit(options.messageLimit());
    if (parsedUpload.candidates().size() > messageLimit) {
      throw new ValidationException("Upload parsed " + parsedUpload.candidates().size()
          + " messages, which is above the configured limit of " + messageLimit);
    }

    List<String> errors = new ArrayList<>();
    List<UploadMessagePreview> previews = previews(topicDescription, parsedUpload.candidates(), options);
    int produced = 0;

    if (!options.dryRun()) {
      ProducerRecordCreator producerRecordCreator = deserializationService.producerRecordCreator(
          cluster,
          topicDescription.name(),
          options.keySerde(),
          options.valueSerde(),
          Map.of(),
          Map.of()
      );

      try (KafkaProducer<byte[], byte[]> producer = createProducer(cluster, Map.of())) {
        int index = 0;
        for (UploadCandidate candidate : parsedUpload.candidates()) {
          try {
            Integer partition = resolveUploadPartition(topicDescription, options, index);
            producer.send(producerRecordCreator.create(
                topicDescription.name(),
                partition,
                candidate.key(),
                candidate.value(),
                uploadHeaders(candidate, options, index)
            )).get();
            produced++;
          } catch (Exception e) {
            errors.add(candidate.entryName() + ": " + e.getMessage());
          }
          index++;
        }
      }
    }

    return new UploadMessagesResult(
        options.dryRun(),
        files.size(),
        parsedUpload.entriesRead(),
        parsedUpload.candidates().size(),
        produced,
        errors.size(),
        parsedUpload.fileResults(),
        previews,
        errors
    );
  }

  private record ParsedUpload(List<UploadCandidate> candidates,
                              List<UploadMessagesFileResult> fileResults,
                              int entriesRead) {
  }

  private ParsedUpload parseUploadFiles(List<UploadSourceFile> files, UploadMessagesOptions options) {
    List<UploadCandidate> candidates = new ArrayList<>();
    List<UploadMessagesFileResult> fileResults = new ArrayList<>();
    int entriesRead = 0;

    for (UploadSourceFile file : files) {
      int before = candidates.size();
      List<UploadSourceFile> extractedFiles = extractFiles(file);
      entriesRead += extractedFiles.size();
      for (UploadSourceFile extractedFile : extractedFiles) {
        candidates.addAll(parseUploadContent(
            file.fileName(),
            extractedFile.fileName(),
            extractedFile.content(),
            options
        ));
      }
      fileResults.add(new UploadMessagesFileResult(
          file.fileName(),
          extractedFiles.size(),
          candidates.size() - before
      ));
    }

    return new ParsedUpload(candidates, fileResults, entriesRead);
  }

  private List<UploadSourceFile> extractFiles(UploadSourceFile file) {
    if (!isZip(file)) {
      return List.of(file);
    }

    List<UploadSourceFile> entries = new ArrayList<>();
    try (ZipInputStream zipInputStream = new ZipInputStream(
        new ByteArrayInputStream(file.content()), StandardCharsets.UTF_8)) {
      for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {
        if (!entry.isDirectory()) {
          entries.add(new UploadSourceFile(entry.getName(), zipInputStream.readAllBytes()));
        }
        zipInputStream.closeEntry();
      }
    } catch (IOException e) {
      throw new ValidationException("Failed to read ZIP file " + file.fileName() + ": " + e.getMessage());
    }
    return entries;
  }

  private boolean isZip(UploadSourceFile file) {
    String lowerFileName = file.fileName().toLowerCase(Locale.ROOT);
    byte[] content = file.content();
    return lowerFileName.endsWith(".zip") || (content.length > 3 && content[0] == 'P' && content[1] == 'K');
  }

  private List<UploadCandidate> parseUploadContent(String sourceFile,
                                                   String entryName,
                                                   byte[] content,
                                                   UploadMessagesOptions options) {
    String text = new String(content, StandardCharsets.UTF_8);
    String key = keyFor(sourceFile, entryName, options.keyMode());
    List<String> values = switch (options.parseMode()) {
      case FILE_PER_MESSAGE -> text.isEmpty() ? List.of() : List.of(text);
      case TEXT_LINES -> text.lines().filter(line -> !line.isBlank()).toList();
      case NDJSON -> parseNdjson(entryName, text);
      case JSON_ARRAY -> parseJsonArray(entryName, text);
    };

    List<UploadCandidate> candidates = new ArrayList<>();
    for (int i = 0; i < values.size(); i++) {
      candidates.add(new UploadCandidate(sourceFile, entryName, key, values.get(i), i));
    }
    return candidates;
  }

  private List<String> parseNdjson(String entryName, String text) {
    List<String> values = new ArrayList<>();
    text.lines()
        .filter(line -> !line.isBlank())
        .forEach(line -> values.add(compactJson(entryName, line)));
    return values;
  }

  private List<String> parseJsonArray(String entryName, String text) {
    try {
      JsonNode json = OBJECT_MAPPER.readTree(text);
      if (!json.isArray()) {
        return List.of(OBJECT_MAPPER.writeValueAsString(json));
      }
      List<String> values = new ArrayList<>();
      json.forEach(item -> values.add(jsonToString(entryName, item)));
      return values;
    } catch (IOException e) {
      throw new ValidationException(entryName + " is not valid JSON: " + e.getMessage());
    }
  }

  private String compactJson(String entryName, String jsonLine) {
    try {
      return OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER.readTree(jsonLine));
    } catch (IOException e) {
      throw new ValidationException(entryName + " contains invalid NDJSON: " + e.getMessage());
    }
  }

  private String jsonToString(String entryName, JsonNode json) {
    try {
      return OBJECT_MAPPER.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      throw new ValidationException(entryName + " contains invalid JSON: " + e.getMessage());
    }
  }

  private List<UploadMessagePreview> previews(TopicDescription topicDescription,
                                              List<UploadCandidate> candidates,
                                              UploadMessagesOptions options) {
    List<UploadMessagePreview> previews = new ArrayList<>();
    int previewCount = Math.min(10, candidates.size());
    for (int i = 0; i < previewCount; i++) {
      UploadCandidate candidate = candidates.get(i);
      previews.add(new UploadMessagePreview(
          candidate.sourceFile(),
          candidate.entryName(),
          resolveUploadPartition(topicDescription, options, i),
          candidate.key(),
          candidate.value().getBytes(StandardCharsets.UTF_8).length,
          preview(candidate.value())
      ));
    }
    return previews;
  }

  private String preview(String value) {
    String normalized = value.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "…";
  }

  private String keyFor(String sourceFile, String entryName, UploadKeyMode keyMode) {
    return switch (keyMode) {
      case NONE -> null;
      case FILE_NAME -> sourceFile;
      case ENTRY_NAME -> entryName;
    };
  }

  private Map<String, String> uploadHeaders(UploadCandidate candidate,
                                            UploadMessagesOptions options,
                                            int index) {
    Map<String, String> headers = new LinkedHashMap<>(parseUploadHeaders(options.headersJson()));
    if (options.includeMetadataHeaders()) {
      headers.put("kafbat-upload-file", candidate.sourceFile());
      headers.put("kafbat-upload-entry", candidate.entryName());
      headers.put("kafbat-upload-source-index", Integer.toString(candidate.sourceIndex()));
      headers.put("kafbat-upload-index", Integer.toString(index));
    }
    return headers;
  }

  private Map<String, String> parseUploadHeaders(@Nullable String headersJson) {
    if (headersJson == null || headersJson.isBlank()) {
      return Map.of();
    }
    try {
      JsonNode root = OBJECT_MAPPER.readTree(headersJson);
      if (!root.isObject()) {
        throw new ValidationException("Headers must be a JSON object");
      }
      Map<String, String> headers = new LinkedHashMap<>();
      root.properties().forEach(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
      return headers;
    } catch (IOException e) {
      throw new ValidationException("Headers JSON is invalid: " + e.getMessage());
    }
  }

  private Integer resolveUploadPartition(TopicDescription topicDescription,
                                         UploadMessagesOptions options,
                                         int messageIndex) {
    List<Integer> partitions = uploadTargetPartitions(topicDescription, options);
    return switch (options.partitionStrategy()) {
      case ANY -> null;
      case SELECTED -> options.selectedPartition();
      case RANDOM -> partitions.get(ThreadLocalRandom.current().nextInt(partitions.size()));
      case EVEN -> partitions.get(messageIndex % partitions.size());
    };
  }

  private List<Integer> uploadTargetPartitions(TopicDescription topicDescription, UploadMessagesOptions options) {
    Set<Integer> actualPartitions = topicDescription.partitions()
        .stream()
        .map(partition -> partition.partition())
        .collect(Collectors.toSet());
    if (actualPartitions.isEmpty()) {
      throw new ValidationException("Topic has no partitions");
    }

    List<Integer> requested = options.partitionStrategy() == UploadPartitionStrategy.SELECTED
        ? List.of(Optional.ofNullable(options.selectedPartition())
            .orElseThrow(() -> new ValidationException("Selected partition is required")))
        : options.targetPartitions();

    List<Integer> partitions = requested == null || requested.isEmpty()
        ? actualPartitions.stream().sorted().toList()
        : requested.stream().distinct().sorted().toList();

    if (!actualPartitions.containsAll(partitions)) {
      throw new ValidationException("One or more requested partitions do not exist");
    }
    return partitions;
  }

  private int resolveUploadMessageLimit(@Nullable Integer messageLimit) {
    int resolved = Optional.ofNullable(messageLimit).orElse(DEFAULT_UPLOAD_MESSAGE_LIMIT);
    if (resolved < 1 || resolved > MAX_UPLOAD_MESSAGE_LIMIT) {
      throw new ValidationException("Upload message limit must be between 1 and " + MAX_UPLOAD_MESSAGE_LIMIT);
    }
    return resolved;
  }

  private void validateUploadSerdes(UploadMessagesOptions options) {
    if (options.keySerde() == null || options.keySerde().isBlank()) {
      throw new ValidationException("Key serde is required");
    }
    if (options.valueSerde() == null || options.valueSerde().isBlank()) {
      throw new ValidationException("Value serde is required");
    }
  }

  private String messageFileName(TopicMessageDTO message, String safeTopic, DownloadFormat format) {
    String extension = format == DownloadFormat.JSON ? ".json" : ".txt";
    return message.getOffset() + "Offset-" + message.getPartition() + "Partition-" + safeTopic + "-Topic"
        + extension;
  }

  private String messageContent(String topic, TopicMessageDTO message, DownloadFormat format) {
    return switch (format) {
      case TEXT -> messageText(topic, message);
      case JSON -> messageJson(topic, message);
      case VALUE_ONLY -> nullToEmpty(message.getValue());
    };
  }

  private String messageJson(String topic, TopicMessageDTO message) {
    Map<String, Object> exportedMessage = new LinkedHashMap<>();
    exportedMessage.put("topic", topic);
    exportedMessage.put("partition", message.getPartition());
    exportedMessage.put("offset", message.getOffset());
    exportedMessage.put("timestamp", message.getTimestamp() == null ? null : message.getTimestamp().toString());
    exportedMessage.put("timestampType", message.getTimestampType());
    exportedMessage.put("key", message.getKey());
    exportedMessage.put("headers", Optional.ofNullable(message.getHeaders()).orElse(Map.of()));
    exportedMessage.put("keySize", message.getKeySize());
    exportedMessage.put("valueSize", message.getValueSize());
    exportedMessage.put("keySerde", message.getKeySerde());
    exportedMessage.put("valueSerde", message.getValueSerde());
    exportedMessage.put("value", message.getValue());
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(exportedMessage);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize Kafka message", e);
    }
  }

  private String messageText(String topic, TopicMessageDTO message) {
    StringBuilder text = new StringBuilder();
    text.append("Topic: ").append(topic).append(System.lineSeparator());
    text.append("Partition: ").append(message.getPartition()).append(System.lineSeparator());
    text.append("Offset: ").append(message.getOffset()).append(System.lineSeparator());
    text.append("Timestamp: ").append(message.getTimestamp()).append(System.lineSeparator());
    text.append("Timestamp type: ").append(message.getTimestampType()).append(System.lineSeparator());
    text.append("Key: ").append(nullToEmpty(message.getKey())).append(System.lineSeparator());
    text.append("Headers: ").append(Optional.ofNullable(message.getHeaders()).orElse(Map.of()))
        .append(System.lineSeparator());
    text.append("Key size: ").append(message.getKeySize()).append(System.lineSeparator());
    text.append("Value size: ").append(message.getValueSize()).append(System.lineSeparator());
    text.append(System.lineSeparator()).append("Payload:").append(System.lineSeparator());
    text.append(nullToEmpty(message.getValue()));
    return text.toString();
  }

  private String nullToEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }

  private String safeZipName(String value) {
    String safe = value.replaceAll(INVALID_ZIP_ENTRY_CHARS, "_").replace("..", "_").trim();
    return safe.isEmpty() ? "topic" : safe;
  }

}
