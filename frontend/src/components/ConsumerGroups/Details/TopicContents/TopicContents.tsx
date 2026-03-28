import { Table } from 'components/common/table/Table/Table.styled';
import TableHeaderCell from 'components/common/table/TableHeaderCell/TableHeaderCell';
import {
  ConsumerGroupTopicLag,
  ConsumerGroupTopicPartition,
  SortOrder,
} from 'generated-sources';
import React from 'react';
import { LagTrend, LagTrendComponent } from 'lib/consumerGroups';

type OrderByKey = keyof ConsumerGroupTopicPartition;
interface Headers {
  title: string;
  orderBy: OrderByKey | undefined;
}

const TABLE_HEADERS_MAP: Headers[] = [
  { title: 'Partition', orderBy: 'partition' },
  { title: 'Consumer ID', orderBy: 'consumerId' },
  { title: 'Host', orderBy: 'host' },
  { title: 'Consumer Lag', orderBy: 'consumerLag' },
  { title: 'Current Offset', orderBy: 'currentOffset' },
  { title: 'End offset', orderBy: 'endOffset' },
];

const ipV4ToNum = (ip?: string) => {
  if (typeof ip === 'string' && ip.length !== 0) {
    const withoutSlash = ip.indexOf('/') !== -1 ? ip.slice(1) : ip;
    return Number(
      withoutSlash
        .split('.')
        .map((octet) => `000${octet}`.slice(-3))
        .join('')
    );
  }
  return 0;
};

type ComparatorFunction<T> = (
  valueA: T,
  valueB: T,
  order: SortOrder,
  property?: keyof T
) => number;

const numberComparator: ComparatorFunction<ConsumerGroupTopicPartition> = (
  valueA,
  valueB,
  order,
  property
) => {
  if (property !== undefined) {
    return order === SortOrder.ASC
      ? Number(valueA[property]) - Number(valueB[property])
      : Number(valueB[property]) - Number(valueA[property]);
  }
  return 0;
};

const ipComparator: ComparatorFunction<ConsumerGroupTopicPartition> = (
  valueA,
  valueB,
  order
) =>
  order === SortOrder.ASC
    ? ipV4ToNum(valueA.host) - ipV4ToNum(valueB.host)
    : ipV4ToNum(valueB.host) - ipV4ToNum(valueA.host);

const consumerIdComparator: ComparatorFunction<ConsumerGroupTopicPartition> = (
  valueA,
  valueB,
  order
) => {
  if (valueA.consumerId && valueB.consumerId) {
    if (order === SortOrder.ASC) {
      if (valueA.consumerId?.toLowerCase() > valueB.consumerId?.toLowerCase()) {
        return 1;
      }
    }

    if (order === SortOrder.DESC) {
      if (valueB.consumerId?.toLowerCase() > valueA.consumerId?.toLowerCase()) {
        return -1;
      }
    }
  }

  return 0;
};

interface Props {
  topicPartitions: ConsumerGroupTopicPartition[];
  partitionLags: ConsumerGroupTopicLag | undefined;
  partitionTrends: Record<string, LagTrend>;
}

const TopicContents: React.FC<Props> = ({
  topicPartitions,
  partitionTrends,
  partitionLags,
}) => {
  const [orderBy, setOrderBy] = React.useState<OrderByKey>('partition');
  const [sortOrder, setSortOrder] = React.useState<SortOrder>(SortOrder.DESC);

  const handleOrder = React.useCallback((columnName: string | null) => {
    if (typeof columnName === 'string') {
      setOrderBy(columnName as OrderByKey);
      setSortOrder((prevOrder) =>
        prevOrder === SortOrder.DESC ? SortOrder.ASC : SortOrder.DESC
      );
    }
  }, []);

  const sortedConsumers = React.useMemo(() => {
    if (orderBy && sortOrder) {
      const isNumberProperty =
        orderBy === 'partition' ||
        orderBy === 'currentOffset' ||
        orderBy === 'endOffset' ||
        orderBy === 'consumerLag';

      let comparator: ComparatorFunction<ConsumerGroupTopicPartition>;
      if (isNumberProperty) {
        comparator = numberComparator;
      }

      if (orderBy === 'host') {
        comparator = ipComparator;
      }

      if (orderBy === 'consumerId') {
        comparator = consumerIdComparator;
      }

      return topicPartitions.sort((a, b) =>
        comparator(a, b, sortOrder, orderBy)
      );
    }
    return topicPartitions;
  }, [orderBy, sortOrder, topicPartitions]);

  return (
    <Table isFullwidth>
      <thead>
        <tr>
          {TABLE_HEADERS_MAP.map((header) => (
            <TableHeaderCell
              key={header.orderBy}
              title={header.title}
              orderBy={orderBy}
              sortOrder={sortOrder}
              orderValue={header.orderBy}
              handleOrderBy={handleOrder}
            />
          ))}
        </tr>
      </thead>
      <tbody>
        {sortedConsumers.map((topicPartition) => (
          <tr key={topicPartition.partition}>
            <td>{topicPartition.partition}</td>
            <td className="break-spaces">{topicPartition.consumerId}</td>
            <td>{topicPartition.host}</td>
            <td>
              <LagTrendComponent
                lag={
                  partitionLags?.partitions?.[String(topicPartition.partition)]
                }
                trend={partitionTrends[topicPartition.partition]}
              />
            </td>
            <td>{topicPartition.currentOffset}</td>
            <td>{topicPartition.endOffset}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

export default TopicContents;
