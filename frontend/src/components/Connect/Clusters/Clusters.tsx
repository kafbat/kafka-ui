import React from 'react';
import { Connect } from 'generated-sources';

import List from './ui/List/List';
import ClustersStatistics from './ui/Statistics/Statistics';

const connects: Connect[] = [
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
  return (
    <>
      <ClustersStatistics connects={connects} />
      <List connects={connects} />
    </>
  );
};

export default KafkaConnectClustersPage;
