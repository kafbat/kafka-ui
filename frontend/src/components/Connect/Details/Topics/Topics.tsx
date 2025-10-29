import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterTopicPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import { useConnector } from 'lib/hooks/api/kafkaConnect';
import Table from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';
import { Link } from 'react-router-dom';
import { TableKeyLink } from 'components/common/table/Table/TableKeyLink.styled';

const Topics = () => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();

  const { data: raw } = useConnector(routerProps);

  const connector = { ...raw, topics: ['wikimedia.recentchange.connect'] };

  const columns: ColumnDef<{ topicName: string }>[] = [
    {
      header: 'Topic',
      accessorKey: 'topicName',
      cell: ({ getValue }) => {
        const topicName = getValue<string>();

        return (
          <TableKeyLink>
            <Link to={clusterTopicPath(routerProps.clusterName, topicName)}>
              {topicName}
            </Link>
          </TableKeyLink>
        );
      },
    },
  ];

  const tableData = (connector?.topics ?? []).map((topicName) => ({
    topicName,
  }));

  return (
    <Table columns={columns} data={tableData} emptyMessage="No topics found" />
  );
};

export default Topics;
