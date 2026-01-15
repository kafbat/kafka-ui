import { consumerGroupsApiClient as api } from 'lib/api';
import {
  useMutation,
  useQuery,
  useQueryClient,
  useSuspenseQuery,
} from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';
import {
  ConsumerGroup,
  ConsumerGroupLag,
  ConsumerGroupOffsetsReset,
  ConsumerGroupOrdering,
  ConsumerGroupsLagResponse,
  SortOrder,
} from 'generated-sources';
import { showSuccessAlert } from 'lib/errorHandling';
import { useEffect, useRef } from 'react';

export type ConsumerGroupID = ConsumerGroup['groupId'];

type UseConsumerGroupsProps = {
  clusterName: ClusterName;
  orderBy?: ConsumerGroupOrdering;
  sortOrder?: SortOrder;
  page?: number;
  perPage?: number;
  search: string;
  fts?: boolean;
};

type UseConsumerGroupDetailsProps = {
  clusterName: ClusterName;
  consumerGroupID: ConsumerGroupID;
};

export function useConsumerGroups(props: UseConsumerGroupsProps) {
  const { clusterName, ...rest } = props;
  return useQuery({
    queryKey: ['clusters', clusterName, 'consumerGroups', rest],
    queryFn: () => api.getConsumerGroupsPage(props),
    placeholderData: (previousData) => previousData,
  });
}

export function useConsumerGroupDetails(props: UseConsumerGroupDetailsProps) {
  const { clusterName, consumerGroupID } = props;
  return useSuspenseQuery({
    queryKey: ['clusters', clusterName, 'consumerGroups', consumerGroupID],
    queryFn: () => api.getConsumerGroup({ clusterName, id: consumerGroupID }),
  });
}

export const useDeleteConsumerGroupMutation = ({
  clusterName,
  consumerGroupID,
}: UseConsumerGroupDetailsProps) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      api.deleteConsumerGroup({ clusterName, id: consumerGroupID }),
    onSuccess: () => {
      showSuccessAlert({
        message: `Consumer ${consumerGroupID} group deleted`,
      });
      queryClient.invalidateQueries({
        queryKey: ['clusters', clusterName, 'consumerGroups'],
      });
    },
  });
};

export const useResetConsumerGroupOffsetsMutation = ({
  clusterName,
  consumerGroupID,
}: UseConsumerGroupDetailsProps) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (props: ConsumerGroupOffsetsReset) =>
      api.resetConsumerGroupOffsets({
        clusterName,
        id: consumerGroupID,
        consumerGroupOffsetsReset: props,
      }),
    onSuccess: () => {
      showSuccessAlert({
        message: `Consumer ${consumerGroupID} group offsets reset`,
      });
      queryClient.invalidateQueries({
        queryKey: ['clusters', clusterName, 'consumerGroups'],
      });
    },
  });
};

export const useDeleteConsumerGroupOffsetsMutation = ({
  clusterName,
  consumerGroupID,
}: UseConsumerGroupDetailsProps) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (topicName: string) =>
      api.deleteConsumerGroupOffsets({
        clusterName,
        id: consumerGroupID,
        topicName,
      }),
    onSuccess: (_, topicName) => {
      showSuccessAlert({
        message: `Consumer ${consumerGroupID} group offsets in topic ${topicName} deleted`,
      });
      queryClient.invalidateQueries({
        queryKey: ['clusters', clusterName, 'consumerGroups'],
      });
    },
  });
};

interface UseGetConsumerGroupsLagProps {
  clusterName: string;
  ids: string[];
  pollingIntervalSec?: number;
}

export function useGetConsumerGroupsLag({
  clusterName,
  pollingIntervalSec = 0,
  ids,
}: UseGetConsumerGroupsLagProps) {
  const pollingEnabled = pollingIntervalSec > 0;
  const lastUpdateRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    lastUpdateRef.current = undefined;
  }, [clusterName, ids.join(',')]);

  return useQuery({
    queryKey: ['clusters', clusterName, 'consumerGroupsLag', ids],
    queryFn: async () => {
      const response = await api.getConsumerGroupsLag({
        clusterName,
        ids,
        lastUpdate: lastUpdateRef.current,
      });

      lastUpdateRef.current = response.updateTimestamp;
      return response;
    },
    enabled: ids.length > 0,
    refetchInterval: pollingEnabled ? pollingIntervalSec * 1000 : false,
    refetchOnWindowFocus: false,

    select: (data) => {
      const filtered: Record<string, ConsumerGroupLag | undefined> = {};
      ids.forEach((id) => {
        filtered[id] = data.consumerGroups?.[id];
      });

      return {
        updateTimestamp: data.updateTimestamp,
        consumerGroups: filtered,
      } satisfies ConsumerGroupsLagResponse;
    },
  });
}
