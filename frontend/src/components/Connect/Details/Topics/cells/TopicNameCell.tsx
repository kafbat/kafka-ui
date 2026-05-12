import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterTopicPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import { CellContext } from '@tanstack/react-table';
import { TableKeyLink } from 'components/common/table/Table/TableKeyLink.styled';
import { Link } from 'react-router-dom';

type TopicNameCellProps = CellContext<{ topicName: string }, unknown>;

export const TopicNameCell = ({ getValue }: TopicNameCellProps) => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const topicName = getValue<string>();

  return (
    <TableKeyLink>
      <Link to={clusterTopicPath(routerProps.clusterName, topicName)}>
        {topicName}
      </Link>
    </TableKeyLink>
  );
};
