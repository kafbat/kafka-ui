import React from 'react';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import { Connect } from 'generated-sources';

import List from './ui/List/List';
import Statistics from './ui/Statistics/Statistics';

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
    <div>
      <ResourcePageHeading text="Kafka connect clusters" />
      <Statistics connects={connects} />
      <List connects={connects} />
    </div>
  );
};

export default KafkaConnectClustersPage;
