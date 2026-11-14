import {
  ConsumerGroupTopicLag,
  ConsumerGroupTopicPartition,
} from 'generated-sources';
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
      meta: { csvFn: (row) => String(row.consumerLag) },
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

export const getConsumerGroupTopicPartitionsTableData = ({
  partitions = [],
  searchQuery,
  lags,
}: {
  partitions: ConsumerGroupTopicPartition[];
  searchQuery: string;
  lags: Record<string, ConsumerGroupTopicLag | undefined> | undefined;
}): ConsumerGroupTopicPartition[] => {
  if (partitions.length === 0) return [];

  return partitions
    .filter((p) => p.topic.includes(searchQuery))
    .map((p) => ({
      ...p,
      consumerLag:
        lags?.[p.topic]?.partitions?.[String(p.partition)] ?? p.consumerLag,
    }));
};

export const getConsumerGroupTopicPartitionsTableColumns = () => {
  const columnHelper = createColumnHelper<ConsumerGroupTopicPartition>();

  return [
    columnHelper.accessor('topic', { header: 'Topic', size: 800 }),
    columnHelper.accessor('partition', { header: 'Partition', size: 100 }),
    columnHelper.accessor('consumerId', { header: 'Consumer ID', size: 350 }),
    columnHelper.accessor('host', { header: 'Host', size: 150 }),
    columnHelper.accessor('consumerLag', {
      header: 'Consumer lag',
      size: 150,
      meta: {
        csvFn: (row) =>
          row.consumerLag === undefined || row.consumerLag === null
            ? 'N/A'
            : String(row.consumerLag),
      },
    }),
    columnHelper.accessor('currentOffset', {
      header: 'Current offset',
      size: 150,
    }),
    columnHelper.accessor('endOffset', { header: 'End offset', size: 150 }),
  ];
};
