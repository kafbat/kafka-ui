import React, { FC, useMemo } from 'react';
import * as Statistics from 'components/common/Statistics';
import { useFilteredConnectors } from 'components/Connect/model/FilteredConnectorsProvider';

import { computeStatistics } from './models/computeStatistics';

interface ConnectorsStatisticsProps {
  isLoading: boolean;
}
const ConnectorsStatistics: FC<ConnectorsStatisticsProps> = ({ isLoading }) => {
  const connectors = useFilteredConnectors();

  const statistics = useMemo(() => {
    return computeStatistics(connectors);
  }, [connectors]);

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
