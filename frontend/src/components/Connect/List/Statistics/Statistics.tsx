import React, { FC, useMemo } from 'react';
import * as Statistics from 'components/common/Statistics';
import { FullConnectorInfo } from 'generated-sources';

import { computeStatistics } from './models/computeStatistics';

interface ConnectorsStatisticsProps {
  connectors: FullConnectorInfo[];
  isLoading: boolean;
}
const ConnectorsStatistics: FC<ConnectorsStatisticsProps> = ({
  connectors,
  isLoading,
}) => {
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
