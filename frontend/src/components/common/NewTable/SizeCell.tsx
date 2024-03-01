import React from 'react';
import { CellContext } from '@tanstack/react-table';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';

const SizeCell = <TData, TValue>({
  getValue,
  precision = 0,
}: CellContext<TData, TValue> & {
  precision?: number;
}) => {
  return (
    <BytesFormatted value={getValue<string | number>()} precision={precision} />
  );
};

export default SizeCell;
