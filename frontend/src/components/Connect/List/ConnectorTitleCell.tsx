import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { FullConnectorInfo } from 'generated-sources';
import { NavLink } from 'react-router-dom';

export const ConnectorTitleCell: React.FC<
  CellContext<FullConnectorInfo, unknown>
> = ({ row: { original } }) => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { name, connect } = original;
  return (
    <NavLink
      to={clusterConnectConnectorPath(clusterName, connect, name)}
      title={name}
    >
      {name}
    </NavLink>
  );
};
