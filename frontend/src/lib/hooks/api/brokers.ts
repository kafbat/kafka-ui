import { brokersApiClient as api } from 'lib/api';
import {
  useMutation,
  useQuery,
  useQueryClient,
  UseQueryOptions,
  useSuspenseQuery,
} from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';
import { BrokerConfigItem, Broker } from 'generated-sources';
import { apiFetch, ServerResponse } from 'lib/errorHandling';

interface UpdateBrokerConfigProps {
  name: string;
  brokerConfigItem: BrokerConfigItem;
}

export function useBrokers(
  clusterName: ClusterName,
  queryOptions?: Omit<
    UseQueryOptions<Broker[], ServerResponse>,
    'queryKey' | 'queryFn'
  >
) {
  return useQuery<Broker[], ServerResponse>({
    queryKey: ['clusters', clusterName, 'brokers'],
    queryFn: () => apiFetch(() => api.getBrokers({ clusterName })),
    refetchInterval: 5000,
    ...queryOptions,
  });
}

export function useBrokerMetrics(clusterName: ClusterName, brokerId: number) {
  return useSuspenseQuery({
    queryKey: ['clusters', clusterName, 'brokers', brokerId, 'metrics'],
    queryFn: () =>
      api.getBrokersMetrics({
        clusterName,
        id: brokerId,
      }),
  });
}

export function useBrokerLogDirs(clusterName: ClusterName, brokerId: number) {
  return useSuspenseQuery({
    queryKey: ['clusters', clusterName, 'brokers', brokerId, 'logDirs'],
    queryFn: () =>
      api.getAllBrokersLogdirs({
        clusterName,
        broker: [brokerId],
      }),
  });
}

export function useBrokerConfig(clusterName: ClusterName, brokerId: number) {
  return useSuspenseQuery({
    queryKey: ['clusters', clusterName, 'brokers', brokerId, 'settings'],
    queryFn: () =>
      api.getBrokerConfig({
        clusterName,
        id: brokerId,
      }),
  });
}

export function useUpdateBrokerConfigByName(
  clusterName: ClusterName,
  brokerId: number
) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateBrokerConfigProps) =>
      api.updateBrokerConfigByName({
        ...payload,
        clusterName,
        id: brokerId,
      }),
    onSuccess: () =>
      client.invalidateQueries({
        queryKey: ['clusters', clusterName, 'brokers', brokerId, 'settings'],
      }),
  });
}
