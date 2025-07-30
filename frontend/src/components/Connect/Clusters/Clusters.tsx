import React from 'react';
import { Connect } from 'generated-sources';
import { useConnects } from 'lib/hooks/api/kafkaConnect';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';

import ClustersStatistics from './ui/Statistics/Statistics';
import List from './ui/List/List';

const testConnects: Connect[] = [
  {
    name: 'local',
    connectorsCount: 5,
    failedConnectorsCount: 0,
    tasksCount: 5,
    failedTasksCount: 0,
  },
  {
    name: 'Cluster name 2',
    connectorsCount: 5,
    failedConnectorsCount: 1,
    tasksCount: 6,
    failedTasksCount: 3,
  },
];

const KafkaConnectClustersPage = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connects, isLoading } = useConnects(clusterName, true);
  return (
    <>
      <ClustersStatistics connects={connects ?? []} isLoading={isLoading} />
      <List connects={connects ?? []} />
    </>
  );
};

export default KafkaConnectClustersPage;
