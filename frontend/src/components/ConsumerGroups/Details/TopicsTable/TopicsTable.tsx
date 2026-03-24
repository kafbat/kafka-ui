import React from 'react';
import Table from 'components/common/NewTable';
import { ConsumerGroupTopicPartition } from 'generated-sources';
import { useSearchParams } from 'react-router-dom';
import TopicContents from 'components/ConsumerGroups/Details/TopicContents/TopicContents';

import {
  getConsumerGroupTopicsTableColumns,
  getConsumerGroupTopicsTableData,
} from './lib/utils';
import groupBy from 'lib/functions/groupBy';

type TopicsTableProps = {
  partitions: ConsumerGroupTopicPartition[];
};

export const TopicsTable = ({ partitions }: TopicsTableProps) => {
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get('q') || '';

  const columns = getConsumerGroupTopicsTableColumns();
  const tableData = getConsumerGroupTopicsTableData({
    partitions,
    searchQuery,
  });
  const partitionsByTopic = groupBy(
    partitions || [],
    'topic'
  );

  return (
    <Table
      getRowCanExpand={() => true}
      columns={columns}
      data={tableData}
      emptyMessage="No topics"
      renderSubComponent={
        (row) => {
          const topicName = row.row.original.topicName;
          return <TopicContents topicPartitions={partitionsByTopic[topicName] ?? []} />
        }
      }
    />
  );
};
