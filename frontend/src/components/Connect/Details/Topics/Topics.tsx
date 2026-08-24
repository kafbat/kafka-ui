import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { RouterParamsClusterConnectConnector } from 'lib/paths';
import { useConnector } from 'lib/hooks/api/kafkaConnect';
import Table from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';

import { TopicNameCell } from './cells/TopicNameCell';

const columns: ColumnDef<{ topicName: string }>[] = [
  {
    header: 'Topic',
    accessorKey: 'topicName',
    cell: TopicNameCell,
  },
];

const Topics = () => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();

  const { data: connector } = useConnector(routerProps);

  const tableData = (connector?.topics ?? []).map((topicName) => ({
    topicName,
  }));

  return (
    <Table columns={columns} data={tableData} emptyMessage="No topics found" />
  );
};

export default Topics;
