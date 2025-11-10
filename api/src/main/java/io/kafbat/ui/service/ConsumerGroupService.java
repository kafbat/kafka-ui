package io.kafbat.ui.service;

import com.google.common.collect.Streams;
import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.emitter.EnhancedConsumer;
import io.kafbat.ui.model.ConsumerGroupOrderingDTO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.ToIntFunction;
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
    var groupNames = descriptions.stream().map(ConsumerGroupDescription::groupId).toList();
    // 1. getting committed offsets for all groups
    return ac.listConsumerGroupOffsets(groupNames, null)
        .flatMap((Table<String, TopicPartition, Long> committedOffsets) -> {
          // 2. getting end offsets for partitions with committed offsets
          return ac.listOffsets(committedOffsets.columnKeySet(), OffsetSpec.latest(), false)
              .map(endOffsets ->
                  descriptions.stream()
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
    return ac.listConsumerGroupOffsets(groupIds, partitions).map(offsets ->
        filtered.stream().filter(g ->
            isConsumerGroupRelatesToTopic(topic, g, offsets.containsRow(g.groupId()))
        ).map(g ->
            InternalTopicConsumerGroup.create(topic, g, offsets.row(g.groupId()), endOffsets)
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

  public record ConsumerGroupsPage(List<InternalConsumerGroup> consumerGroups, int totalPages) {
  }

  private record GroupWithDescr(InternalConsumerGroup icg, ConsumerGroupDescription cgd) {
  }

  public Mono<ConsumerGroupsPage> getConsumerGroupsPage(
      KafkaCluster cluster,
      int pageNum,
      int perPage,
      @Nullable String search,
      Boolean fts,
      ConsumerGroupOrderingDTO orderBy,
      SortOrderDTO sortOrderDto) {
    return adminClientService.get(cluster).flatMap(ac ->
        ac.listConsumerGroups()
            .map(listing -> filterGroups(listing, search, fts)
            )
            .flatMapIterable(lst -> lst)
            .filterWhen(cg -> accessControlService.isConsumerGroupAccessible(cg.groupId(), cluster.getName()))
            .collectList()
            .flatMap(allGroups ->
                loadSortedDescriptions(ac, allGroups, pageNum, perPage, orderBy, sortOrderDto)
                    .flatMap(descriptions -> getConsumerGroups(ac, descriptions)
                        .map(page -> new ConsumerGroupsPage(
                            page,
                            (allGroups.size() / perPage) + (allGroups.size() % perPage == 0 ? 0 : 1))))));
  }

  private Collection<ConsumerGroupListing> filterGroups(Collection<ConsumerGroupListing> groups, String search,
                                                        Boolean useFts) {
    ClustersProperties.ClusterFtsProperties ftsProperties = clustersProperties.getFts();
    boolean fts = ftsProperties.use(useFts);
    ConsumerGroupFilter filter = new ConsumerGroupFilter(groups, fts, ftsProperties.getConsumers());
    return filter.find(search);
  }

  private Mono<List<ConsumerGroupDescription>> loadSortedDescriptions(ReactiveAdminClient ac,
                                                                      List<ConsumerGroupListing> groups,
                                                                      int pageNum,
                                                                      int perPage,
                                                                      ConsumerGroupOrderingDTO orderBy,
                                                                      SortOrderDTO sortOrderDto) {
    return switch (orderBy) {
      case NAME -> {
        Comparator<ConsumerGroupListing> comparator = Comparator.comparing(ConsumerGroupListing::groupId);
        yield loadDescriptionsByListings(ac, groups, comparator, pageNum, perPage, sortOrderDto);
      }
      case STATE -> {
        ToIntFunction<ConsumerGroupListing> statesPriorities =
            cg -> switch (cg.state().orElse(ConsumerGroupState.UNKNOWN)) {
                  case STABLE -> 0;
                  case COMPLETING_REBALANCE -> 1;
                  case PREPARING_REBALANCE -> 2;
                  case EMPTY -> 3;
                  case DEAD -> 4;
                  case UNKNOWN -> 5;
                  case ASSIGNING -> 6;
                  case RECONCILING -> 7;
                };
        var comparator = Comparator.comparingInt(statesPriorities);
        yield loadDescriptionsByListings(ac, groups, comparator, pageNum, perPage, sortOrderDto);
      }
      case MEMBERS -> {
        var comparator = Comparator.<ConsumerGroupDescription>comparingInt(cg -> cg.members().size());
        var groupNames = groups.stream().map(ConsumerGroupListing::groupId).toList();
        yield ac.describeConsumerGroups(groupNames)
            .map(descriptions ->
                sortAndPaginate(descriptions.values(), comparator, pageNum, perPage, sortOrderDto).toList());
      }
      case MESSAGES_BEHIND -> {

        Comparator<GroupWithDescr> comparator = Comparator.comparingLong(gwd ->
            gwd.icg.getConsumerLag() == null ? 0L : gwd.icg.getConsumerLag());

        yield loadDescriptionsByInternalConsumerGroups(ac, groups, comparator, pageNum, perPage, sortOrderDto);
      }

      case TOPIC_NUM -> {

        Comparator<GroupWithDescr> comparator = Comparator.comparingInt(gwd -> gwd.icg.getTopicNum());

        yield loadDescriptionsByInternalConsumerGroups(ac, groups, comparator, pageNum, perPage, sortOrderDto);

      }
    };
  }

  private Mono<List<ConsumerGroupDescription>> loadDescriptionsByListings(ReactiveAdminClient ac,
                                                                          List<ConsumerGroupListing> listings,
                                                                          Comparator<ConsumerGroupListing> comparator,
                                                                          int pageNum,
                                                                          int perPage,
                                                                          SortOrderDTO sortOrderDto) {
    List<String> sortedGroups = sortAndPaginate(listings, comparator, pageNum, perPage, sortOrderDto)
        .map(ConsumerGroupListing::groupId)
        .toList();
    return ac.describeConsumerGroups(sortedGroups)
        .map(descrMap -> sortedGroups.stream().map(descrMap::get).toList());
  }

  private <T> Stream<T> sortAndPaginate(Collection<T> collection,
                                        Comparator<T> comparator,
                                        int pageNum,
                                        int perPage,
                                        SortOrderDTO sortOrderDto) {
    return collection.stream()
        .sorted(sortOrderDto == SortOrderDTO.ASC ? comparator : comparator.reversed())
        .skip((long) (pageNum - 1) * perPage)
        .limit(perPage);
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
        return ac.describeConsumerGroups(notFound)
            .map(descriptions -> {
              result.addAll(descriptions.values());
              return result;
            });
      } else {
        return Mono.just(result);
      }
    } else {
      return ac.describeConsumerGroups(groupNames)
          .map(descriptions -> List.copyOf(descriptions.values()));
    }
  }




  private Mono<List<ConsumerGroupDescription>> loadDescriptionsByInternalConsumerGroups(
      ReactiveAdminClient ac,
      List<ConsumerGroupListing> groups,
      Comparator<GroupWithDescr> comparator,
      int pageNum,
      int perPage,
      SortOrderDTO sortOrderDto) {
    var groupNames = groups.stream().map(ConsumerGroupListing::groupId).toList();

    return ac.describeConsumerGroups(groupNames)
        .flatMap(descriptionsMap -> {
              List<ConsumerGroupDescription> descriptions = descriptionsMap.values().stream().toList();
              return getConsumerGroups(ac, descriptions)
                  .map(icg -> Streams.zip(icg.stream(), descriptions.stream(), GroupWithDescr::new).toList())
                  .map(gwd -> sortAndPaginate(gwd, comparator, pageNum, perPage, sortOrderDto)
                      .map(GroupWithDescr::cgd).toList());
            }
        );

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
