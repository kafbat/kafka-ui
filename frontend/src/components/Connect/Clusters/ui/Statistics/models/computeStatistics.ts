import { Connect } from 'generated-sources';

interface Statistic {
  clustersCount: number;
  connectorsCount: number;
  failedConnectorsCount: number;
  tasksCount: number;
  failedTasksCount: number;
}
export const computeStatistic = (connects: Connect[]): Statistic => {
  const clustersCount = connects.length;
  let connectorsCount = 0;
  let failedConnectorsCount = 0;
  let tasksCount = 0;
  let failedTasksCount = 0;

  connects.forEach((connect) => {
    connectorsCount += connect.connectorsCount ?? 0;
    failedConnectorsCount += connect.failedConnectorsCount ?? 0;
    tasksCount += connect.failedConnectorsCount ?? 0;
    failedTasksCount += connect.failedConnectorsCount ?? 0;
  });

  return {
    clustersCount,
    connectorsCount,
    failedConnectorsCount,
    tasksCount,
    failedTasksCount,
  };
};
