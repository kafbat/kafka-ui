import React, { useEffect, useRef } from 'react';
import Table from 'components/common/NewTable';
import {
  ConsumerGroupsLagResponse,
  ConsumerGroupTopicPartition,
} from 'generated-sources';
import { useSearchParams } from 'react-router-dom';
import TopicContents from 'components/ConsumerGroups/Details/TopicContents/TopicContents';
import groupBy from 'lib/functions/groupBy';
import { computeLagTrends, LagTrend } from 'lib/consumerGroups';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterGroupParam } from 'lib/paths';

import {
  getConsumerGroupTopicsTableColumns,
  getConsumerGroupTopicsTableData,
} from './lib/utils';

type TopicsTableProps = {
  partitions: ConsumerGroupTopicPartition[];
  isLagFetched: boolean;
  consumerGroupsLag: ConsumerGroupsLagResponse | undefined;
  pollingIntervalSec: number;
};

export const TopicsTable = ({
  partitions,
  isLagFetched,
  consumerGroupsLag,
  pollingIntervalSec,
}: TopicsTableProps) => {
  const [searchParams] = useSearchParams();
  const searchQuery = searchParams.get('q') || '';
  const routeParams = useAppParams<ClusterGroupParam>();
  const { consumerGroupID } = routeParams;

  const prevLagRef = useRef<Record<string, number | undefined>>({});
  const [lagTrends, setLagTrends] = React.useState<Record<string, LagTrend>>(
    {}
  );

  useEffect(() => {
    if (isLagFetched && !!consumerGroupsLag) {
      const nextTrends = computeLagTrends(
        prevLagRef.current,
        consumerGroupsLag.consumerGroups?.[consumerGroupID]?.topics ?? {},
        (lag) => lag,
        pollingIntervalSec > 0
      );

      setLagTrends(nextTrends);
    }
  }, [consumerGroupsLag, isLagFetched]);

  const columns = getConsumerGroupTopicsTableColumns();
  const tableData = getConsumerGroupTopicsTableData({
    partitions,
    searchQuery,
    lags: consumerGroupsLag?.consumerGroups?.[consumerGroupID]?.topics,
    lagTrends,
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
          <TopicContents topicPartitions={partitionsByTopic[topicName] ?? []} />
        );
      }}
    />
  );
};
