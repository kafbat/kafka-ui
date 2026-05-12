import React, { useMemo } from 'react';
import { Connect } from 'generated-sources';
import * as Statistics from 'components/common/Statistics';

import { computeStatistic } from './models/computeStatistics';

type Props = { connects: Connect[]; isLoading: boolean };
const ClustersStatistics = ({ connects, isLoading }: Props) => {
  const statistic = useMemo(() => {
    return computeStatistic(connects);
  }, [connects]);
  return (
    <Statistics.Container>
      <Statistics.Item
        title="Clusters"
        count={statistic.clustersCount}
        isLoading={isLoading}
      />
      <Statistics.Item
        title="Connectors"
        count={statistic.connectorsCount}
        warningCount={statistic.failedConnectorsCount}
        isLoading={isLoading}
      />
      <Statistics.Item
        title="Tasks"
        count={statistic.tasksCount}
        warningCount={statistic.failedTasksCount}
        isLoading={isLoading}
      />
    </Statistics.Container>
  );
};

export default ClustersStatistics;
