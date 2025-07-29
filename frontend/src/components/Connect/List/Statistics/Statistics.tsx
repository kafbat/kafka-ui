import React, { useMemo } from 'react';
import * as Statistics from 'components/common/Statistics';
import { FullConnectorInfo } from 'generated-sources';

import { computeStatistics } from './models/computeStatistics';

type Props = {
  connectors: FullConnectorInfo[];
  isLoading: boolean;
};
const ConnectorsStatistics = ({ connectors, isLoading }: Props) => {
  const statistics = useMemo(() => {
    return computeStatistics(connectors);
  }, [connectors]);

  return (
    <Statistics.Container>
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
