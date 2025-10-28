import React from 'react';
import { LinkCell } from 'components/common/NewTable';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import { CellContext } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';

type KafkaConnectLinkCellProps = CellContext<FullConnectorInfo, string>;

export const KafkaConnectLinkCell = ({
  getValue,
  row,
}: KafkaConnectLinkCellProps) => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { connect, name } = row.original;
  const value = getValue();

  return (
    <LinkCell
      value={value}
      to={clusterConnectConnectorPath(clusterName, connect, name)}
      wordBreak
    />
  );
};
