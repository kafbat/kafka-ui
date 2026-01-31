import React, { useEffect, useRef, useState } from 'react';
import { clusterConsumerGroupsPath, RouteParamsClusterTopic } from 'lib/paths';
import { ConsumerGroup } from 'generated-sources';
import useAppParams from 'lib/hooks/useAppParams';
import { useTopicConsumerGroups } from 'lib/hooks/api/topics';
import { ColumnDef } from '@tanstack/react-table';
import Table, { LinkCell, TagCell } from 'components/common/NewTable';
import Search from 'components/common/Search/Search';
import { RefreshRateSelect } from 'components/common/RefreshRateSelect/RefreshRateSelect';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import { computeLagTrends, LagTrend } from 'lib/consumerGroups';
import { LagContainer } from 'components/ConsumerGroups/styled';
import { useGetConsumerGroupsLag } from 'lib/hooks/api/consumers';

import * as S from './TopicConsumerGroups.styled';

const TopicConsumerGroups: React.FC = () => {
  const [keyword, setKeyword] = React.useState('');
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();

  const { data = [] } = useTopicConsumerGroups({
    clusterName,
    topicName,
  });

  const consumerGroups = React.useMemo(
    () =>
      data.filter(
        (item) => item.groupId.toLocaleLowerCase().indexOf(keyword) > -1
      ),
    [data, keyword]
  );

  const [pollingIntervalSec] = useLocalStorage('topics-refresh-rate', 0);
  const prevLagRef = useRef<Record<string, number | undefined>>({});
  const [lagTrends, setLagTrends] = useState<Record<string, LagTrend>>({});

  const { data: consumerGroupsLag, isSuccess } = useGetConsumerGroupsLag({
    clusterName,
    ids: consumerGroups.map((cg) => cg.groupId),
    pollingIntervalSec,
  });

  useEffect(() => {
    if (isSuccess && !!consumerGroupsLag) {
      const nextTrends = computeLagTrends(
        prevLagRef.current,
        consumerGroupsLag.consumerGroups ?? {},
        (cg) => cg?.topics?.[topicName],
        pollingIntervalSec > 0
      );

      setLagTrends(nextTrends);
    }
  }, []);

  useEffect(() => {
    prevLagRef.current = {};
    setLagTrends({});
  }, [topicName]);

  const columns: ColumnDef<ConsumerGroup>[] = [
    {
      header: 'Consumer Group ID',
      accessorKey: 'groupId',
      enableSorting: false,
      // eslint-disable-next-line react/no-unstable-nested-components
      cell: ({ row }) => (
        <LinkCell
          value={row.original.groupId}
          to={`${clusterConsumerGroupsPath(clusterName)}/${
            row.original.groupId
          }`}
        />
      ),
    },
    {
      header: 'Active Consumers',
      accessorKey: 'members',
      enableSorting: false,
    },
    {
      header: 'Consumer Lag',
      accessorKey: 'consumerLag',
      enableSorting: false,
      // eslint-disable-next-line react/no-unstable-nested-components
      cell: ({ row }) => {
        const { groupId } = row.original;
        const lag =
          consumerGroupsLag?.consumerGroups?.[groupId]?.topics?.[topicName];
        const trend = lagTrends[groupId];

        if (lag == null) return 'N/A';

        let trendElement = null;

        if (trend === 'up') {
          trendElement = '▲';
        } else if (trend === 'down') {
          trendElement = '▼';
        }

        return (
          <LagContainer $lagTrend={trend}>
            <span>{lag}</span>
            {trendElement && <span>{trendElement}</span>}
          </LagContainer>
        );
      },
    },
    {
      header: 'Coordinator',
      accessorKey: 'coordinator',
      enableSorting: false,
      cell: ({ getValue }) => {
        const coordinator = getValue<ConsumerGroup['coordinator']>();
        if (coordinator === undefined) {
          return 0;
        }
        return coordinator.id;
      },
    },
    {
      header: 'State',
      accessorKey: 'state',
      enableSorting: false,
      cell: TagCell,
    },
  ];

  return (
    <>
      <S.SearchWrapper>
        <Search
          onChange={setKeyword}
          placeholder="Search by Consumer Name"
          value={keyword}
        />

        <RefreshRateSelect storageKey="topics-refresh-rate" />
      </S.SearchWrapper>
      <Table
        columns={columns}
        data={consumerGroups}
        enableSorting
        emptyMessage="No active consumer groups"
      />
    </>
  );
};

export default TopicConsumerGroups;
