import React from 'react';
import { CellContext } from '@tanstack/react-table';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AsAny = any;

const SizeCell = <TValue = unknown,>({
  getValue,
  row,
  renderSegments = false,
  precision = 0,
}: CellContext<AsAny, TValue> & {
  renderSegments?: boolean;
  precision?: number;
}) => (
  <>
    <BytesFormatted value={getValue<string | number>()} precision={precision} />
    {renderSegments ? `, ${row?.original.count} segment(s)` : null}
  </>
);

export default SizeCell;
