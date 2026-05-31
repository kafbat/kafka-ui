import { useGetConsumerGroupsLag } from 'lib/hooks/api/consumers';
import { useEffect, useMemo, useRef } from 'react';
import {
  buildNextLagMap,
  buildNextPartitionsLagMap,
  computeLagTrends,
  computePartitionsLagTrends,
  LagMap,
  LagTrends,
  PartitionsLagMap,
} from 'lib/consumerGroups';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';

export const useConsumerGroupsLagTrends = ({
  clusterName,
  ids,
  storageKey,
  includePartitions = false,
}: {
  clusterName: string;
  ids: string[];
  storageKey: string;
  includePartitions?: boolean;
}) => {
  const [pollingIntervalSec] = useLocalStorage(storageKey, 0);

  const { data: consumerGroupsLag, isSuccess: isLagFetched } =
    useGetConsumerGroupsLag({
      clusterName,
      ids,
      pollingIntervalSec,
      includePartitions,
    });

  const prevLagRef = useRef<{
    groups: LagMap;
    topics: LagMap;
    partitions: PartitionsLagMap;
  }>({ groups: {}, topics: {}, partitions: {} });

  useEffect(() => {
    prevLagRef.current = { groups: {}, topics: {}, partitions: {} };
  }, [clusterName, ...ids]);

  const lagData = useMemo(() => {
    if (!isLagFetched || !consumerGroupsLag) return null;

    const groups = consumerGroupsLag.consumerGroups ?? {};
    return { groups };
  }, [isLagFetched, consumerGroupsLag]);

  const groupId = ids[0];

  const lagTrends = useMemo(() => {
    const empty: LagTrends = {
      groupLagTrends: {},
      topicsLagTrends: {},
      partitionsLagTrends: {},
    };

    if (!lagData) return empty;

    const isPolling = pollingIntervalSec > 0;
    const { groups } = lagData;

    const groupLagTrends = computeLagTrends(
      prevLagRef.current.groups,
      groups,
      (cg) => cg?.lag,
      isPolling
    );

    if (!includePartitions) {
      return { ...empty, groupLagTrends };
    }

    const topics = groups[groupId]?.topics ?? {};
    const topicPartitions = groups[groupId]?.topicPartitions ?? {};

    return {
      groupLagTrends,
      topicsLagTrends: computeLagTrends(
        prevLagRef.current.topics,
        topics,
        (lag) => lag,
        isPolling
      ),
      partitionsLagTrends: computePartitionsLagTrends(
        prevLagRef.current.partitions,
        topicPartitions,
        isPolling
      ),
    };
  }, [lagData, pollingIntervalSec, includePartitions, groupId]);

  useEffect(() => {
    if (!lagData) return;

    const { groups } = lagData;
    const nextGroups = buildNextLagMap(groups, (cg) => cg?.lag);

    if (!includePartitions) {
      prevLagRef.current = { groups: nextGroups, topics: {}, partitions: {} };
      return;
    }

    const topics = groups[groupId]?.topics ?? {};
    const topicPartitions = groups[groupId]?.topicPartitions ?? {};

    prevLagRef.current = {
      groups: nextGroups,
      topics: buildNextLagMap(topics, (lag) => lag),
      partitions: buildNextPartitionsLagMap(topicPartitions),
    };
  }, [lagData, includePartitions, groupId]);

  return { consumerGroupsLag, lagTrends, pollingIntervalSec };
};
