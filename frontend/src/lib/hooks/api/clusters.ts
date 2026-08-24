import { clustersApiClient as api } from 'lib/api';
import { useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';

export function useClusters() {
  return useQuery({
    queryKey: ['clusters'],
    queryFn: () => api.getClusters(),
  });
}
export function useClusterStats(clusterName: ClusterName) {
  return useSuspenseQuery({
    queryKey: ['clusterStats', clusterName],
    queryFn: () => api.getClusterStats({ clusterName }),
    refetchInterval: 5000,
  });
}
