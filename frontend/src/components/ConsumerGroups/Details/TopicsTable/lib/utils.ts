import { ConsumerGroupTopicPartition } from 'generated-sources';
import { createColumnHelper } from '@tanstack/react-table';
import { NA } from 'components/Brokers/BrokersList/lib';
import * as Cell from 'components/ConsumerGroups/Details/TopicsTable/cells/cells';
import { LagTrend } from 'lib/consumerGroups';

import { ConsumerGroupTopicsTableRow } from './types';

const getConsumerLagByTopic = (partitions: ConsumerGroupTopicPartition[]) =>
  partitions.reduce<Record<string, number[]>>(
    (acc, p) => ({
      ...acc,
      [p.topic]: [...(acc[p.topic] ?? []), p.consumerLag ?? 0],
    }),
    {}
  );

const calculateConsumerLag = (lags: number[]) => {
  const nonNullLags = lags.filter((x) => x != null);
  return nonNullLags.length === 0 ? NA : nonNullLags.reduce((a, v) => a + v, 0);
};

export const getConsumerGroupTopicsTableData = ({
  partitions = [],
  searchQuery,
  lagTrends,
  lags,
}: {
  partitions: ConsumerGroupTopicPartition[];
  searchQuery: string;
  lagTrends: Record<string, LagTrend>;
  lags: Record<string, number | undefined> | undefined;
}): ConsumerGroupTopicsTableRow[] => {
  if (partitions.length === 0) return [];

  const grouped = getConsumerLagByTopic(partitions);
  return Object.entries(grouped)
    .filter(([topic]) => topic.includes(searchQuery))
    .map(([topic, partitionLags]) => ({
      topicName: topic,
      consumerLag: lags?.[topic] ?? calculateConsumerLag(partitionLags),
      lagTrend: lagTrends?.[topic] ?? 'none',
    }));
};

export const getConsumerGroupTopicsTableColumns = () => {
  const columnHelper = createColumnHelper<ConsumerGroupTopicsTableRow>();

  return [
    columnHelper.accessor('topicName', {
      header: 'Topic',
      cell: Cell.TopicName,
      size: 800,
    }),
    columnHelper.accessor('consumerLag', {
      header: 'Consumer lag',
      cell: Cell.ConsumerLag,
      size: 350,
    }),
    columnHelper.accessor('topicName', {
      id: 'actions',
      header: undefined,
      cell: Cell.Actions,
      size: 10,
    }),
  ];
};
