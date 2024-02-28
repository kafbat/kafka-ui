import React from 'react';
import { CellContext } from '@tanstack/react-table';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';

const SizeCellCount = <TData extends { count?: number | undefined }, TValue>({
  getValue,
  row,
  precision = 0,
}: CellContext<TData, TValue> & {
  precision?: number;
}) => {
  return (
    <>
      <BytesFormatted
        value={getValue<string | number>()}
        precision={precision}
      />
      {`, ${row.original.count} segment(s)`}
    </>
  );
};

export default SizeCellCount;
