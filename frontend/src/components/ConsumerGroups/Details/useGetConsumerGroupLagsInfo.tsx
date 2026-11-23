import { useConsumerGroupsLagTrends } from 'components/ConsumerGroups/lib/useConsumerGroupsLagTrends';

export const useGetConsumerGroupLagsInfo = ({
  clusterName,
  consumerGroupID,
}: {
  clusterName: string;
  consumerGroupID: string;
}) => {
  const { consumerGroupsLag, lagTrends } = useConsumerGroupsLagTrends({
    clusterName,
    ids: [consumerGroupID],
    storageKey: `consumer-group-${consumerGroupID}-refresh-rate`,
    includePartitions: true,
  });

  const groupInfo = consumerGroupsLag?.consumerGroups[consumerGroupID];

  return {
    consumerGroupLagInfo: {
      lag: groupInfo?.lag,
      trend: lagTrends.groupLagTrends[consumerGroupID],
    },
    topicsLagInfo: {
      lags: groupInfo?.topics ?? {},
      trends: lagTrends.topicsLagTrends ?? {},
    },
    partitionsLagInfo: {
      lags: groupInfo?.topicPartitions ?? {},
      trends: lagTrends.partitionsLagTrends ?? {},
    },
  };
};
