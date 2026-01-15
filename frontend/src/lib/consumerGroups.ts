export type LagTrend = 'up' | 'down' | 'same' | 'none';

export function computeLagTrends<T>(
  prevLagMap: Record<string, number | undefined>,
  source: Record<string, T | undefined>,
  selectLag: (value: T | undefined) => number | undefined,
  pollingEnabled = true
): Record<string, LagTrend> {
  if (!pollingEnabled) return {};

  const trends: Record<string, LagTrend> = {};

  Object.entries(source).forEach(([key, value]) => {
    const prev = prevLagMap[key];
    const next = selectLag(value);

    if (prev == null || next == null) {
      trends[key] = 'none';
    } else if (next > prev) {
      trends[key] = 'up';
    } else if (next < prev) {
      trends[key] = 'down';
    } else {
      trends[key] = 'same';
    }

    // eslint-disable-next-line no-param-reassign
    prevLagMap[key] = next;
  });

  return trends;
}
