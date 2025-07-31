import React from 'react';
import { useConnects } from 'lib/hooks/api/kafkaConnect';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';

import ClustersStatistics from './ui/Statistics/Statistics';
import List from './ui/List/List';

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
