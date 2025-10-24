import React, { useMemo } from 'react';
import * as Statistics from 'components/common/Statistics';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';

import { computeStatistics } from './models/computeStatistics';

const ConnectorsStatistics = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connectors = [], isLoading } = useConnectors(clusterName);

  const statistics = useMemo(() => computeStatistics(connectors), [connectors]);

  return (
    <Statistics.Container role="group">
      <Statistics.Item
        title="Connectors"
        count={statistics.connectorsCount}
        warningCount={statistics.failedConnectorsCount}
        isLoading={isLoading}
      />
      <Statistics.Item
        title="Tasks"
        count={statistics.tasksCount}
        warningCount={statistics.failedTasksCount}
        isLoading={isLoading}
      />
    </Statistics.Container>
  );
};

export default ConnectorsStatistics;
