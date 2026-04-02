import React from 'react';
import styled from 'styled-components';

export type LagTrend = 'up' | 'down' | 'same' | 'none';
export type LagValue = number | undefined;
export type LagMap = Record<string, LagValue>;
export type PartitionsLagMap = Record<string, LagMap>;
export type TopicPartitions = Record<
  string,
  { partitions?: LagMap } | undefined
>;
export type LagTrends = {
  groupLagTrends: Record<string, LagTrend>;
  topicsLagTrends: Record<string, LagTrend>;
  partitionsLagTrends: Record<string, Record<string, LagTrend>>;
};

export function computeSingleLagTrend(
  prev: LagValue,
  next: LagValue
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
  prevLagMap: LagMap,
  source: Record<string, T | undefined>,
  selectLag: (value: T | undefined) => LagValue,
  pollingEnabled = true
): Record<string, LagTrend> {
  if (!pollingEnabled) return {};

  return Object.fromEntries(
    Object.keys(source).map((key) => [
      key,
      computeSingleLagTrend(prevLagMap[key], selectLag(source[key])),
    ])
  );
}

export function computePartitionsLagTrends(
  prevPartitionsMap: PartitionsLagMap,
  topicPartitions: TopicPartitions,
  isPolling: boolean
): Record<string, Record<string, LagTrend>> {
  return Object.fromEntries(
    Object.entries(topicPartitions).map(([topicName, topicLag]) => [
      topicName,
      computeLagTrends(
        prevPartitionsMap[topicName] ?? {},
        topicLag?.partitions ?? {},
        (lag) => lag,
        isPolling
      ),
    ])
  );
}

export function buildNextLagMap<T>(
  source: Record<string, T | undefined>,
  selectLag: (value: T | undefined) => LagValue
): LagMap {
  return Object.fromEntries(
    Object.keys(source).map((key) => [key, selectLag(source[key])])
  );
}

export function buildNextPartitionsLagMap(
  topicPartitions: TopicPartitions
): PartitionsLagMap {
  return Object.fromEntries(
    Object.entries(topicPartitions).map(([topicName, topicLag]) => [
      topicName,
      buildNextLagMap(topicLag?.partitions ?? {}, (lag) => lag),
    ])
  );
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
