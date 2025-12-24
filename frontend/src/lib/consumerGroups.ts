import { ConsumerGroupsLagResponse } from 'generated-sources';

export type LagTrend = 'up' | 'down' | 'same' | 'none';

export function computeLagTrends(
  prevLagMap: Record<string, number | undefined>,
  nextLagResponse?: ConsumerGroupsLagResponse,
  pollingEnabled = true
): Record<string, LagTrend> {
  if (!pollingEnabled || !nextLagResponse) return {};

  const trends: Record<string, LagTrend> = {};

  Object.entries(nextLagResponse.consumerGroups).forEach(
    ([groupId, lagData]) => {
      const prev = prevLagMap[groupId];
      const next = lagData?.lag;

      if (prev == null || next == null) {
        trends[groupId] = 'none';
      } else if (next > prev) {
        trends[groupId] = 'up';
      } else if (next < prev) {
        trends[groupId] = 'down';
      } else {
        trends[groupId] = 'same';
      }

      // eslint-disable-next-line no-param-reassign
      prevLagMap[groupId] = next;
    }
  );

  return trends;
}
