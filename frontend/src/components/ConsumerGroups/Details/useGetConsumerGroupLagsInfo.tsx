import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import { useGetConsumerGroupsLag } from 'lib/hooks/api/consumers';
import { useMemo, useRef } from 'react';
import { computeLagTrends } from 'lib/consumerGroups';

type GroupId = string;
type TopicName = string;
type PartitionName = number;
type LagValue = number | undefined;

export const useGetConsumerGroupLagsInfo = ({
  consumerGroupID,
  clusterName,
}: {
  clusterName: string;
  consumerGroupID: string;
}) => {
  const [pollingIntervalSec] = useLocalStorage(
    `consumer-group-${consumerGroupID}-refresh-rate`,
    0
  );

  const { data: consumerGroupsLag, isSuccess: isLagFetched } =
    useGetConsumerGroupsLag({
      clusterName,
      ids: [consumerGroupID],
      pollingIntervalSec,
      includePartitions: true,
    });

  const prevLagRef = useRef<{
    groups: Record<GroupId, LagValue>;
    topics: Record<TopicName, LagValue>;
    partitions: Record<TopicName, Record<PartitionName, LagValue>>;
  }>({ groups: {}, topics: {}, partitions: {} });

  const lagTrends = useMemo(() => {
    if (!isLagFetched || !consumerGroupsLag) {
      return {
        groupLagTrends: {},
        topicsLagTrends: {},
        partitionsLagTrends: {},
      };
    }

    const groups = consumerGroupsLag.consumerGroups ?? {};
    const topics = groups[consumerGroupID]?.topics ?? {};
    const topicPartitions = groups[consumerGroupID]?.topicPartitions ?? {};
    const isPolling = pollingIntervalSec > 0;

    const partitionsLagTrends = Object.fromEntries(
      Object.entries(topicPartitions).map(([topicName, topicLag]) => {
        const partitions = topicLag?.partitions ?? {};

        if (!prevLagRef.current.partitions[topicName]) {
          prevLagRef.current.partitions[topicName] = {};
        }

        const trends = computeLagTrends(
          prevLagRef.current.partitions[topicName],
          partitions,
          (lag) => lag,
          isPolling
        );
        return [topicName, trends];
      })
    );

    return {
      groupLagTrends: computeLagTrends(
        prevLagRef.current.groups,
        groups,
        (cg) => cg?.lag,
        isPolling
      ),
      topicsLagTrends: computeLagTrends(
        prevLagRef.current.topics,
        topics,
        (lag) => lag,
        isPolling
      ),
      partitionsLagTrends,
    };
  }, [consumerGroupsLag, isLagFetched]);

  const groupInfo = consumerGroupsLag?.consumerGroups[consumerGroupID];

  return {
    consumerGroupLagInfo: {
      lag: groupInfo?.lag,
      trend: lagTrends.groupLagTrends[consumerGroupID],
    },
    topicsLagInfo: {
      lags: groupInfo?.topics ?? {},
      trends: lagTrends.topicsLagTrends,
    },
    partitionsLagInfo: {
      lags: groupInfo?.topicPartitions ?? {},
      trends: lagTrends.partitionsLagTrends,
    },
  };
};
