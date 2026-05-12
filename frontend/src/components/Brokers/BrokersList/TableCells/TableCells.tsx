import React from 'react';
import { BrokersTableRow } from 'components/Brokers/BrokersList/lib/types';
import { CellContext } from '@tanstack/react-table';
import { LinkCell } from 'components/common/NewTable';
import Tooltip from 'components/common/Tooltip/Tooltip';
import CheckMarkRoundIcon from 'components/common/Icons/CheckMarkRoundIcon';
import { NA } from 'components/Brokers/BrokersList/lib';
import ColoredCell from 'components/common/NewTable/ColoredCell';
import SizeCellCount from 'components/common/NewTable/SizeCellCount';

import * as S from './TableCells.styled';

type BrokerIdProps = CellContext<BrokersTableRow, BrokersTableRow['brokerId']>;

export const BrokerId = ({ getValue, row }: BrokerIdProps) => {
  const { activeControllers } = row.original;
  const brokerId = getValue();

  return (
    <S.RowCell>
      <LinkCell value={`${brokerId}`} to={encodeURIComponent(`${brokerId}`)} />
      {brokerId === activeControllers && (
        <Tooltip
          value={<CheckMarkRoundIcon />}
          content="Active Controller"
          placement="right"
        />
      )}
    </S.RowCell>
  );
};

type DiscUsageProps = CellContext<BrokersTableRow, BrokersTableRow['size']>;

export const DiscUsage = ({
  getValue,
  table,
  cell,
  column,
  renderValue,
  row,
}: DiscUsageProps) => {
  if (getValue() === undefined) return NA;

  return (
    <SizeCellCount
      table={table}
      column={column}
      row={row}
      cell={cell}
      getValue={getValue}
      renderValue={renderValue}
      precision={2}
    />
  );
};

type ScewProps = CellContext<
  BrokersTableRow,
  BrokersTableRow['replicasSkew'] | BrokersTableRow['leadersSkew']
>;

export const getSkewValue = (skew: number | undefined) =>
  skew ? `${skew.toFixed(2)}%` : '-';
export const Skew = ({ getValue }: ScewProps) => {
  const skew = getValue();
  const value = getSkewValue(skew);

  return (
    <ColoredCell
      value={value}
      warn={skew !== undefined && skew >= 10 && skew < 20}
      attention={skew !== undefined && skew >= 20}
    />
  );
};

type InSyncReplicasProps = CellContext<
  BrokersTableRow,
  BrokersTableRow['inSyncReplicas']
>;

export const InSyncReplicas = ({ getValue, row }: InSyncReplicasProps) => {
  const inSyncReplicas = getValue();
  const { replicas } = row.original;
  if (inSyncReplicas === undefined || replicas === undefined) {
    return null;
  }
  return (
    <ColoredCell value={inSyncReplicas} attention={inSyncReplicas < replicas} />
  );
};

type ReplicasProps = CellContext<BrokersTableRow, BrokersTableRow['replicas']>;

export const Replicas = ({ getValue }: ReplicasProps) => {
  return <ColoredCell value={getValue() || ''} />;
};
