import React, { useMemo } from 'react';
import { Connect } from 'generated-sources';

import * as S from './Statistics.styled';
import { computeStatistic } from './models/computeStatistics';
import Statistic from './Statistic/Statistic';

type Props = { connects: Connect[] };
const Statistics = ({ connects }: Props) => {
  const statistic = useMemo(() => {
    return computeStatistic(connects);
  }, [connects]);
  return (
    <S.Container>
      <Statistic title="Clusters" count={statistic.clustersCount} />
      <Statistic
        title="Connectors"
        count={statistic.connectorsCount}
        warningCount={statistic.failedConnectorsCount}
      />
      <Statistic
        title="Tasks"
        count={statistic.tasksCount}
        warningCount={statistic.failedTasksCount}
      />
    </S.Container>
  );
};

export default Statistics;
