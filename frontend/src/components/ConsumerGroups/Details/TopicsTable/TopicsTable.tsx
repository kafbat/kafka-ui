import React from 'react';
import Table from 'components/common/NewTable';
import {
  ConsumerGroupTopicLag,
  ConsumerGroupTopicPartition,
} from 'generated-sources';
import { useSearchParams } from 'react-router-dom';
import TopicContents from 'components/ConsumerGroups/Details/TopicContents/TopicContents';
import groupBy from 'lib/functions/groupBy';
import { LagTrend } from 'lib/consumerGroups';

import {
  getConsumerGroupTopicsTableColumns,
  getConsumerGroupTopicsTableData,
} from './lib/utils';

type TopicsTableProps = {
  partitions: ConsumerGroupTopicPartition[];
  topicsLagInfo: {
    lags: Record<string, number | undefined>;
    trends: Record<string, LagTrend>;
  };
  partitionsLagInfo: {
    lags: Record<string, ConsumerGroupTopicLag | undefined>;
    trends: Record<string, Record<string, LagTrend>>;
  };
};

export const TopicsTable = ({
  partitions,
  topicsLagInfo,
  partitionsLagInfo,
}: TopicsTableProps) => {
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get('q') || '';

  const columns = getConsumerGroupTopicsTableColumns();
  const tableData = getConsumerGroupTopicsTableData({
    partitions,
    searchQuery,
    lags: topicsLagInfo.lags,
    lagTrends: topicsLagInfo.trends,
  });
  const partitionsByTopic = groupBy(partitions || [], 'topic');

  return (
    <Table
      getRowCanExpand={() => true}
      columns={columns}
      data={tableData}
      emptyMessage="No topics"
      renderSubComponent={(row) => {
        const { topicName } = row.row.original;
        return (
          <TopicContents
            topicPartitions={partitionsByTopic[topicName] ?? []}
            partitionLags={partitionsLagInfo.lags[topicName] ?? {}}
            partitionTrends={partitionsLagInfo.trends[topicName] ?? {}}
          />
        );
      }}
    />
  );
};
