import React from 'react';
import { BrokersTableRow } from 'components/Brokers/BrokersList/lib/types';
import { CellContext } from '@tanstack/react-table';
import { LinkCell, SizeCell } from 'components/common/NewTable';
import Tooltip from 'components/common/Tooltip/Tooltip';
import CheckMarkRoundIcon from 'components/common/Icons/CheckMarkRoundIcon';
import { NA } from 'components/Brokers/BrokersList/lib';
import ColoredCell from 'components/common/NewTable/ColoredCell';

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
  if (getValue() === NA) return NA;

  return (
    <SizeCell
      table={table}
      column={column}
      row={row}
      cell={cell}
      getValue={getValue}
      renderValue={renderValue}
      renderSegments
      precision={2}
    />
  );
};

type ScewProps = CellContext<
  BrokersTableRow,
  BrokersTableRow['partitionsSkew'] | BrokersTableRow['leadersSkew']
>;

export const Skew = ({ getValue }: ScewProps) => {
  const skew = getValue();
  const value = skew ? `${skew.toFixed(2)}%` : '-';

  return (
    <ColoredCell
      value={value}
      warn={skew !== undefined && skew >= 10 && skew < 20}
      attention={skew !== undefined && skew >= 20}
    />
  );
};

type OnlinePartitionsProps = CellContext<
  BrokersTableRow,
  BrokersTableRow['onlinePartitionCount']
>;

export const OnlinePartitions = ({ row }: OnlinePartitionsProps) => {
  const { onlinePartitionCount, offlinePartitionCount } = row.original;

  return (
    <ColoredCell
      value={onlinePartitionCount}
      attention={offlinePartitionCount > 0}
    />
  );
};
