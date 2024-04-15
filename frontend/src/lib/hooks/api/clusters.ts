import { clustersApiClient as api } from 'lib/api';
import { useQuery } from '@tanstack/react-query';
import { ClusterName } from 'lib/interfaces/cluster';

export function useClusters() {
  return useQuery(['clusters'], () => api.getClusters(), { suspense: false });
}
export function useClusterStats(clusterName: ClusterName) {
  return useQuery(
    ['clusterStats', clusterName],
    () => api.getClusterStats({ clusterName }),
    { refetchInterval: 5000 }
  );
}
