import { ConsumerGroupTopicPartition } from 'generated-sources';
import {
  getConsumerGroupTopicPartitionsTableColumns,
  getConsumerGroupTopicPartitionsTableData,
} from 'components/ConsumerGroups/Details/TopicsTable/lib/utils';

const partitions: ConsumerGroupTopicPartition[] = [
  {
    topic: 'topic1',
    partition: 0,
    currentOffset: 10,
    endOffset: 20,
    consumerLag: 5,
    consumerId: 'consumer-1',
  },
  {
    topic: 'topic1',
    partition: 1,
    currentOffset: 0,
    endOffset: 0,
    consumerLag: 0,
  },
  {
    topic: 'topic2',
    partition: 0,
    currentOffset: 5,
    endOffset: 10,
    consumerLag: 0,
  },
];

describe('getConsumerGroupTopicPartitionsTableData', () => {
  it('returns an empty array when there are no partitions', () => {
    expect(
      getConsumerGroupTopicPartitionsTableData({
        partitions: [],
        searchQuery: '',
        lags: {},
      })
    ).toEqual([]);
  });

  it('filters partitions by search query', () => {
    const result = getConsumerGroupTopicPartitionsTableData({
      partitions,
      searchQuery: 'topic1',
      lags: {},
    });

    expect(result).toHaveLength(2);
    expect(result.every((p) => p.topic.includes('topic1'))).toBe(true);
  });

  it('enriches consumerLag from the lags map', () => {
    const result = getConsumerGroupTopicPartitionsTableData({
      partitions,
      searchQuery: '',
      lags: {
        topic1: { partitions: { '0': 42, '1': 7 } },
      },
    });

    expect(result[0].consumerLag).toBe(42);
    expect(result[1].consumerLag).toBe(7);
    expect(result[2].consumerLag).toBe(0);
  });

  it('falls back to the partition consumerLag when the lags map has no data', () => {
    const result = getConsumerGroupTopicPartitionsTableData({
      partitions,
      searchQuery: '',
      lags: {},
    });

    expect(result.map((p) => p.consumerLag)).toEqual([5, 0, 0]);
  });
});

describe('getConsumerGroupTopicPartitionsTableColumns', () => {
  const columns = getConsumerGroupTopicPartitionsTableColumns();

  it('defines accessorKey columns with headers', () => {
    expect(columns.map((c) => c.accessorKey)).toEqual([
      'topic',
      'partition',
      'consumerId',
      'host',
      'consumerLag',
      'currentOffset',
      'endOffset',
    ]);
  });

  it('renders N/A for undefined consumer lag in csv', () => {
    const lagColumn = columns.find((c) => c.accessorKey === 'consumerLag');
    const csvFn = lagColumn?.meta?.csvFn;

    expect(csvFn).toBeDefined();
    expect(csvFn?.({ ...partitions[0], consumerLag: undefined })).toBe('N/A');
    expect(csvFn?.({ ...partitions[0], consumerLag: 0 })).toBe('0');
    expect(csvFn?.({ ...partitions[0], consumerLag: 1545 })).toBe('1545');
  });
});
