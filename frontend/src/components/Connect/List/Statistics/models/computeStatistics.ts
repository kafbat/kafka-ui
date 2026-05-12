import { ConnectorState, FullConnectorInfo } from 'generated-sources';

export interface Statistic {
  connectorsCount: number;
  failedConnectorsCount: number;
  tasksCount: number;
  failedTasksCount: number;
}

export const computeStatistics = (
  connectors: FullConnectorInfo[]
): Statistic => {
  const connectorsCount = connectors.length;
  let failedConnectorsCount = 0;
  let tasksCount = 0;
  let failedTasksCount = 0;

  connectors.forEach((connector) => {
    if (connector.status.state === ConnectorState.FAILED) {
      failedConnectorsCount += 1;
    }

    tasksCount += connector.tasksCount ?? 0;
    failedTasksCount += connector.failedTasksCount ?? 0;
  });

  return {
    connectorsCount,
    failedConnectorsCount,
    tasksCount,
    failedTasksCount,
  };
};
