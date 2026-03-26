import React from 'react';
import styled from 'styled-components';

export type LagTrend = 'up' | 'down' | 'same' | 'none';

export function computeSingleLagTrend(
  prev: number | undefined,
  next: number | undefined
): LagTrend {
  if (
    prev === null ||
    prev === undefined ||
    next === null ||
    next === undefined
  ) {
    return 'none';
  }

  if (next > prev) return 'up';
  if (next < prev) return 'down';
  return 'same';
}

export function computeLagTrends<T>(
  prevLagMap: Record<string, number | undefined>,
  source: Record<string, T | undefined>,
  selectLag: (value: T | undefined) => number | undefined,
  pollingEnabled = true
): Record<string, LagTrend> {
  if (!pollingEnabled) return {};

  const trends: Record<string, LagTrend> = {};

  Object.entries(source).forEach(([key, value]) => {
    const next = selectLag(value);
    trends[key] = computeSingleLagTrend(prevLagMap[key], next);
    // eslint-disable-next-line no-param-reassign
    prevLagMap[key] = next;
  });

  return trends;
}

export const LagContainer = styled.div<{ $lagTrend: LagTrend }>`
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme, $lagTrend }) => theme.lag[$lagTrend]};
`;

export const LagTrendComponent = ({
  lag,
  trend,
}: {
  lag: number | string | undefined | null;
  trend?: LagTrend;
}) => {
  if (lag === undefined || lag === null) return 'N/A';

  const effectiveTrend: LagTrend = trend ?? 'none';
  let trendElement = null;

  if (trend === 'up') {
    trendElement = '▲';
  } else if (trend === 'down') {
    trendElement = '▼';
  }

  return (
    <LagContainer $lagTrend={effectiveTrend}>
      <span>{lag}</span>
      {trendElement && <span>{trendElement}</span>}
    </LagContainer>
  );
};
