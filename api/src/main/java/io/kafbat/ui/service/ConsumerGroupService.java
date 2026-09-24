package io.kafbat.ui.service;

import static io.kafbat.ui.util.ConsumerGroupUtil.calculateLag;

import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.emitter.EnhancedConsumer;
import io.kafbat.ui.model.ConsumerGroupLagDTO;
import io.kafbat.ui.model.ConsumerGroupOrderingDTO;
import io.kafbat.ui.model.ConsumerGroupStateDTO;
import io.kafbat.ui.model.ConsumerGroupTopicLagDTO;
import io.kafbat.ui.model.InternalConsumerGroup;
import io.kafbat.ui.model.InternalTopicConsumerGroup;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.index.ConsumerGroupFilter;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.ApplicationMetrics;
import io.kafbat.ui.util.KafkaClientSslPropertiesUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
public class ConsumerGroupService {

  private final AdminClientService adminClientService;
  private final AccessControlService accessControlService;
  private final ClustersProperties clustersProperties;
  private final StatisticsCache statisticsCache;

  private Mono<List<InternalConsumerGroup>> getConsumerGroups(
      ReactiveAdminClient ac,
      List<ConsumerGroupDescription> descriptions) {
    if (descriptions.isEmpty()) {
      return Mono.just(List.of());
    }
    var groupNames = descriptions.stream().map(ConsumerGroupDescription::groupId).toList();
    // 1. getting committed offsets for all groups
    return ac.listAuthorizedConsumerGroupOffsets(groupNames, null)
        .flatMap(authorizedOffsets -> {
          Table<String, TopicPartition, Long> committedOffsets = authorizedOffsets.offsets();
          // 2. getting end offsets for partitions with committed offsets
          Mono<Map<TopicPartition, Long>> endOffsetsRequest = committedOffsets.isEmpty()
              ? Mono.just(Map.of())
              : ac.listOffsets(committedOffsets.columnKeySet(), OffsetSpec.latest(), false);
          return endOffsetsRequest
              .map(endOffsets ->
                  descriptions.stream()
                      .filter(desc -> authorizedOffsets.containsGroup(desc.groupId()))
                      .map(desc -> {
                        var groupOffsets = committedOffsets.row(desc.groupId());
                        var endOffsetsForGroup = new HashMap<>(endOffsets);
                        endOffsetsForGroup.keySet().retainAll(groupOffsets.keySet());
                        // 3. gathering description & offsets
                        return InternalConsumerGroup.create(desc, groupOffsets, endOffsetsForGroup);
                      })
                      .collect(Collectors.toList()));
        });
  }

  private Mono<List<InternalConsumerGroup>> getConsumerGroups(KafkaCluster cluster,
                                                        ReactiveAdminClient ac,
                                                        List<ConsumerGroupDescription> descriptions) {

    Statistics statistics = statisticsCache.get(cluster);
    if (!statistics.getStatus().equals(ServerStatusDTO.ONLINE)) {
      return getConsumerGroups(ac, descriptions);
    }

    Map<String, InternalConsumerGroup> result = new HashMap<>();

    var cachedConsumerGroupsStates = statistics.getClusterState().getConsumerGroupsStates();
    var cachedTopicStates = statistics.getClusterState().getTopicStates();
    var missed = new ArrayList<ConsumerGroupDescription>();

    for (ConsumerGroupDescription consumerGroup : descriptions) {
      Optional<InternalConsumerGroup> internalConsumerGroup =
          getConsumerGroup(consumerGroup, cachedConsumerGroupsStates, cachedTopicStates);
      if (internalConsumerGroup.isPresent()) {
        result.put(consumerGroup.groupId(), internalConsumerGroup.get());
      } else {
        missed.add(consumerGroup);
      }
    }

    Mono<Map<String, InternalConsumerGroup>> consumerGroups = Mono.just(result);
    if (!missed.isEmpty()) {
      consumerGroups = getConsumerGroups(ac, missed).map(r -> {
            var combined = new HashMap<>(result);
            combined.putAll(r.stream().collect(Collectors.toMap(
                InternalConsumerGroup::getGroupId,
                d -> d
            )));
            return combined;
          }
      );
    }

    return consumerGroups.map(res -> descriptions.stream()
        .map(d -> res.get(d.groupId()))
        .filter(Objects::nonNull)
        .toList());
  }

  public Mono<ConsumerGroupsPage> getConsumerGroups(
      KafkaCluster cluster,
      OptionalInt pageNum,
      OptionalInt perPage,
      @Nullable String search,
      Boolean fts,
      ConsumerGroupOrderingDTO orderBy,
      SortOrderDTO sortOrderDto,
      List<ConsumerGroupStateDTO> states) {
    return adminClientService.get(cluster).flatMap(ac ->
        ac.listConsumerGroups()
            .map(listing -> filterGroups(listing, search, fts))
            .map(listing -> filterByState(listing, states))
            .flatMapIterable(lst -> lst)
            .filterWhen(cg -> accessControlService.isConsumerGroupAccessible(cg.groupId(), cluster.getName()))
            .collectList()
            .flatMap(allGroups -> {
              var groupNames = allGroups.stream().map(ConsumerGroupListing::groupId).toList();
              return ac.describeConsumerGroups(groupNames, true)
                  .flatMap(descriptionsById -> {
                    var descriptions = groupNames.stream().map(descriptionsById::get)
                        .filter(Objects::nonNull).toList();
                    return getConsumerGroups(cluster, ac, descriptions)
                        .map(groups -> ConsumerGroupsPage.from(
                            sortAndPaginate(groups, groupComparator(allGroups, orderBy),
                                pageNum, perPage, sortOrderDto).toList(),
                            groups.size(), perPage));
                  });
            })
    );
  }

  private Comparator<InternalConsumerGroup> groupComparator(
      List<ConsumerGroupListing> listings, ConsumerGroupOrderingDTO orderBy) {
    return switch (orderBy) {
      case NAME -> Comparator.comparing(InternalConsumerGroup::getGroupId);
      case STATE -> {
        Map<String, ConsumerGroupListing> listingsById = listings.stream()
            .collect(Collectors.toMap(ConsumerGroupListing::groupId, listing -> listing));
        yield Comparator.comparingInt(group -> statePriority(
            listingsById.get(group.getGroupId()).state().orElse(ConsumerGroupState.UNKNOWN)));
      }
      case MEMBERS -> Comparator.comparingInt(group -> group.getMembers().size());
      case MESSAGES_BEHIND -> Comparator.comparingLong(group ->
          group.getConsumerLag() == null ? 0L : group.getConsumerLag());
      case TOPIC_NUM -> Comparator.comparingInt(InternalConsumerGroup::getTopicNum);
    };
  }

  private int statePriority(ConsumerGroupState state) {
    return switch (state) {
      case STABLE -> 0;
      case COMPLETING_REBALANCE -> 1;
      case PREPARING_REBALANCE -> 2;
      case EMPTY -> 3;
      case DEAD -> 4;
      case UNKNOWN -> 5;
      case ASSIGNING -> 6;
      case RECONCILING -> 7;
    };
  }

  private Optional<InternalConsumerGroup> getConsumerGroup(
      ConsumerGroupDescription consumerGroup,
      Map<String, ScrapedClusterState.ConsumerGroupState> cachedConsumerGroupsStates,
      Map<String, ScrapedClusterState.TopicState> cachedTopicStates) {
    var consumerGroupState = cachedConsumerGroupsStates.get(consumerGroup.groupId());
    if (consumerGroupState != null) {
      Map<TopicPartition, Long> groupOffsets = consumerGroupState.committedOffsets();
      Map<TopicPartition, Long> endOffsets = new HashMap<>();
      boolean cacheComplete = true;

      for (TopicPartition topicPartition : groupOffsets.keySet()) {
        var topicState = cachedTopicStates.get(topicPartition.topic());
        if (topicState == null || !topicState.endOffsets().containsKey(topicPartition.partition())) {
          cacheComplete = false;
          break;
        }
        endOffsets.put(topicPartition, topicState.endOffsets().get(topicPartition.partition()));
      }

      if (cacheComplete) {
        return Optional.of(
            InternalConsumerGroup.create(consumerGroup, groupOffsets, endOffsets)
        );
      }
    }

    return Optional.empty();
  }

  private Collection<ConsumerGroupListing> filterByState(Collection<ConsumerGroupListing> groups,
                                                         List<ConsumerGroupStateDTO> states) {
    if (states.isEmpty()) {
      return groups;
    }
    Set<ConsumerGroupState> kafkaStates = states.stream()
        .map(this::mapToKafkaState)
        .collect(Collectors.toSet());
    return groups.stream()
        .filter(cg -> kafkaStates.contains(cg.state().orElse(ConsumerGroupState.UNKNOWN)))
        .toList();
  }

  private ConsumerGroupState mapToKafkaState(ConsumerGroupStateDTO stateDto) {
    return switch (stateDto) {
      case UNKNOWN -> ConsumerGroupState.UNKNOWN;
      case PREPARING_REBALANCE -> ConsumerGroupState.PREPARING_REBALANCE;
      case COMPLETING_REBALANCE -> ConsumerGroupState.COMPLETING_REBALANCE;
      case STABLE -> ConsumerGroupState.STABLE;
      case DEAD -> ConsumerGroupState.DEAD;
      case EMPTY -> ConsumerGroupState.EMPTY;
    };
  }

  public Mono<List<InternalTopicConsumerGroup>> getConsumerGroupsForTopic(KafkaCluster cluster,
                                                                          String topic) {
    return adminClientService.get(cluster)
        .flatMap(ac -> ac.listTopicOffsets(topic, OffsetSpec.latest(), false)
            .flatMap(endOffsets ->
                describeConsumerGroups(cluster, ac, true).flatMap(groups ->
                    filterConsumerGroups(cluster, ac, groups, topic, endOffsets)
                )
            )
        );
  }

  private Mono<List<InternalTopicConsumerGroup>> filterConsumerGroups(
      KafkaCluster cluster,
      ReactiveAdminClient ac,
      List<ConsumerGroupDescription> groups,
      String topic,
      Map<TopicPartition, Long> endOffsets) {

    Set<ConsumerGroupState> inactiveStates = Set.of(
        ConsumerGroupState.DEAD,
        ConsumerGroupState.EMPTY
    );

    Map<Boolean, List<ConsumerGroupDescription>> partitioned = groups.stream().collect(
        Collectors.partitioningBy((g) -> !inactiveStates.contains(g.state()))
    );

    List<ConsumerGroupDescription> stable = partitioned.get(true).stream()
        .filter(g -> isConsumerGroupRelatesToTopic(topic, g, false))
        .toList();

    List<ConsumerGroupDescription> dead = partitioned.get(false);
    if (!dead.isEmpty()) {
      Statistics statistics = statisticsCache.get(cluster);
      if (statistics.getStatus().equals(ServerStatusDTO.ONLINE)) {
        Map<String, ScrapedClusterState.ConsumerGroupState> consumerGroupsStates =
            statistics.getClusterState().getConsumerGroupsStates();
        dead = dead.stream().filter(g ->
                Optional.ofNullable(consumerGroupsStates.get(g.groupId()))
                    .map(s ->
                            s.committedOffsets().keySet().stream().anyMatch(tp -> tp.topic().equals(topic))
                    ).orElse(false)
        ).toList();
      }
    }

    List<ConsumerGroupDescription> filtered =  new ArrayList<>(stable.size() + dead.size());
    filtered.addAll(stable);
    filtered.addAll(dead);

    List<TopicPartition> partitions = new ArrayList<>(endOffsets.keySet());

    List<String> groupIds = filtered.stream().map(ConsumerGroupDescription::groupId).toList();
    return ac.listAuthorizedConsumerGroupOffsets(groupIds, partitions).map(authorizedOffsets ->
        filtered.stream().filter(g -> authorizedOffsets.containsGroup(g.groupId()))
            .filter(g ->
            isConsumerGroupRelatesToTopic(topic, g, authorizedOffsets.offsets().containsRow(g.groupId()))
        ).map(g ->
            InternalTopicConsumerGroup.create(topic, g, authorizedOffsets.offsets().row(g.groupId()), endOffsets)
        ).toList());
  }

  private boolean isConsumerGroupRelatesToTopic(String topic,
                                                ConsumerGroupDescription description,
                                                boolean hasCommittedOffsets) {
    boolean hasActiveMembersForTopic = description.members()
        .stream()
        .anyMatch(m -> m.assignment().topicPartitions().stream().anyMatch(tp -> tp.topic().equals(topic)));
    return hasActiveMembersForTopic || hasCommittedOffsets;
  }

  public Mono<Tuple2<Map<String, ConsumerGroupLagDTO>, Optional<Long>>> getConsumerGroupsLag(
      KafkaCluster cluster, Collection<String> groupNames, boolean includePartitions, Optional<Long> lastUpdate) {
    Statistics statistics = statisticsCache.get(cluster);

    Map<TopicPartition, Long> endOffsets = statistics.getClusterState().getTopicStates().entrySet().stream()
        .flatMap(e -> e.getValue().endOffsets().entrySet().stream().map(p ->
            Map.entry(new TopicPartition(e.getKey(), p.getKey()), p.getValue()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (statistics.getStatus().equals(ServerStatusDTO.ONLINE)) {
      boolean select = lastUpdate
          .map(t -> statistics.getClusterState().getScrapeFinishedAt().isAfter(Instant.ofEpochMilli(t)))
          .orElse(true);

      if (select) {
        Map<String, ScrapedClusterState.ConsumerGroupState> consumerGroupsStates =
            statistics.getClusterState().getConsumerGroupsStates();

        return Mono.just(
            Tuples.of(
                groupNames.stream()
                    .map(g -> Optional.ofNullable(consumerGroupsStates.get(g)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(g -> Map.entry(g.group(), buildConsumerGroup(g, endOffsets, includePartitions)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                Optional.of(statistics.getClusterState().getScrapeFinishedAt().toEpochMilli())
            )
        );
      }

    }

    return Mono.just(Tuples.of(Map.of(), lastUpdate));
  }

  private ConsumerGroupLagDTO buildConsumerGroup(
      ScrapedClusterState.ConsumerGroupState state,
      Map<TopicPartition, Long> endOffsets,
      boolean includePartitions
  ) {
    var commitedTopicPartitions = Stream.concat(
        state.description().members().stream()
            .flatMap(m ->
                m.assignment().topicPartitions().stream()
                    .map(t -> Map.entry(t, Optional.<Long>empty()))
            ),
        state.committedOffsets().entrySet().stream()
            .map(o -> Map.entry(o.getKey(), Optional.ofNullable(o.getValue())))
    ).collect(
        Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue,
                Collectors.<Optional<Long>>reducing(
                    Optional.empty(),
                    (a, b) -> Stream.of(a, b)
                        .flatMap(Optional::stream)
                        .max(Long::compare)
                )
            )
        )
    );

    Map<TopicPartition, Long> topicPartitionsLags = commitedTopicPartitions.entrySet().stream()
        .map(e ->
            Map.entry(
                e.getKey(),
                calculateLag(e.getValue(), Optional.ofNullable(endOffsets.get(e.getKey()))).orElse(0L)
            )
        ).collect(
            Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.reducing(0L, Map.Entry::getValue, Long::sum)
            )
        );

    Map<String, Long> topicsLags = topicPartitionsLags.entrySet().stream().collect(
            Collectors.groupingBy(
              (e) -> e.getKey().topic(),
              Collectors.reducing(0L, Map.Entry::getValue, Long::sum)
            )
        );

    long lag = topicsLags.values().stream().mapToLong(v -> v).sum();

    Map<String, ConsumerGroupTopicLagDTO> lagByTopicPartition = null;

    if (includePartitions) {
      lagByTopicPartition = topicPartitionsLags.entrySet()
          .stream()
          .collect(Collectors.groupingBy(
              e -> e.getKey().topic(),   // group by topic name
              Collectors.collectingAndThen(
                  Collectors.toMap(
                      e -> String.valueOf(e.getKey().partition()), // partition as String
                      Map.Entry::getValue                          // lag
                  ),
                  ConsumerGroupTopicLagDTO::new
              )
          ));
    }

    return new ConsumerGroupLagDTO(lag, topicsLags, lagByTopicPartition);
  }

  public record ConsumerGroupsPage(List<InternalConsumerGroup> consumerGroups, int totalPages) {
    public static ConsumerGroupsPage from(List<InternalConsumerGroup> groups,
                                          int totalSize,
                                          OptionalInt perPage) {
      return new ConsumerGroupsPage(groups,
          perPage.isPresent() ? (totalSize + perPage.getAsInt() - 1) / perPage.getAsInt()
              : (totalSize == 0 ? 0 : 1)
      );
    }
  }

  private Collection<ConsumerGroupListing> filterGroups(Collection<ConsumerGroupListing> groups, String search,
                                                        Boolean useFts) {
    ClustersProperties.ClusterFtsProperties ftsProperties = clustersProperties.getFts();
    boolean fts = ftsProperties.use(useFts);
    ConsumerGroupFilter filter = new ConsumerGroupFilter(groups, fts, ftsProperties.getConsumers());
    return filter.find(search);
  }

  private <T> Stream<T> sortAndPaginate(Collection<T> collection,
                                        Comparator<T> comparator,
                                        OptionalInt pageNum,
                                        OptionalInt perPage,
                                        SortOrderDTO sortOrderDto) {
    Stream<T> sorted = collection.stream()
        .sorted(sortOrderDto == SortOrderDTO.ASC ? comparator : comparator.reversed());

    if (pageNum.isPresent() && perPage.isPresent()) {
      return sorted
          .skip((long) (pageNum.getAsInt() - 1) * perPage.getAsInt())
          .limit(perPage.getAsInt());
    } else {
      return sorted;
    }
  }

  private Mono<List<ConsumerGroupDescription>> describeConsumerGroups(
      KafkaCluster cluster,
      ReactiveAdminClient ac,
      boolean cache) {
    return ac.listConsumerGroupNames()
        .flatMap(names -> describeConsumerGroups(names, cluster, ac, cache));
  }

  private Mono<List<ConsumerGroupDescription>> describeConsumerGroups(
      List<String> groupNames,
      KafkaCluster cluster,
      ReactiveAdminClient ac,
      boolean cache) {

    Statistics statistics = statisticsCache.get(cluster);

    if (cache && statistics.getStatus().equals(ServerStatusDTO.ONLINE)) {
      List<ConsumerGroupDescription> result = new ArrayList<>();
      List<String> notFound = new ArrayList<>();
      Map<String, ScrapedClusterState.ConsumerGroupState> consumerGroupsStates =
          statistics.getClusterState().getConsumerGroupsStates();
      for (String groupName : groupNames) {
        ScrapedClusterState.ConsumerGroupState consumerGroupState = consumerGroupsStates.get(groupName);
        if (consumerGroupState != null) {
          result.add(consumerGroupState.description());
        } else {
          notFound.add(groupName);
        }
      }
      if (!notFound.isEmpty()) {
        return ac.describeConsumerGroups(notFound, true)
            .map(descriptions -> {
              result.addAll(descriptions.values());
              return result;
            });
      } else {
        return Mono.just(result);
      }
    } else {
      return ac.describeConsumerGroups(groupNames, true)
          .map(descriptions -> List.copyOf(descriptions.values()));
    }
  }




  public Mono<InternalConsumerGroup> getConsumerGroupDetail(KafkaCluster cluster,
                                                            String consumerGroupId) {
    return adminClientService.get(cluster)
        .flatMap(ac -> ac.describeConsumerGroups(List.of(consumerGroupId))
            .filter(m -> m.containsKey(consumerGroupId))
            .map(r -> r.get(consumerGroupId))
            .flatMap(descr ->
                getConsumerGroups(ac, List.of(descr))
                    .filter(groups -> !groups.isEmpty())
                    .map(groups -> groups.get(0))));
  }

  public Mono<Void> deleteConsumerGroupById(KafkaCluster cluster,
                                            String groupId) {
    return adminClientService.get(cluster)
        .flatMap(adminClient -> adminClient.deleteConsumerGroups(List.of(groupId)));
  }

  public Mono<Void> deleteConsumerGroupOffset(KafkaCluster cluster,
                                              String groupId,
                                              String topicName) {
    return adminClientService.get(cluster)
        .flatMap(adminClient -> adminClient.deleteConsumerGroupOffsets(groupId, topicName));
  }

  public EnhancedConsumer createConsumer(KafkaCluster cluster) {
    return createConsumer(cluster, Map.of());
  }

  public EnhancedConsumer createConsumer(KafkaCluster cluster,
                                         Map<String, Object> properties) {
    Properties props = new Properties();
    KafkaClientSslPropertiesUtil.addKafkaSslProperties(cluster.getOriginalProperties().getSsl(), props);
    props.putAll(cluster.getProperties());
    props.putAll(cluster.getConsumerProperties());
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafbat-ui-consumer-" + System.currentTimeMillis());
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
    props.putAll(properties);

    return new EnhancedConsumer(
        props,
        cluster.getPollingSettings().getPollingThrottler(),
        ApplicationMetrics.forCluster(cluster)
    );
  }

}
