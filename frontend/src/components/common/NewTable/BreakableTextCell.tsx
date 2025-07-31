import React from 'react';
import { CellContext } from '@tanstack/react-table';

const BreakableTextCell = <T,>({ getValue }: CellContext<T, unknown>) => {
  return (
    <div
      style={{
        wordBreak: 'break-word',
        whiteSpace: 'pre-wrap',
      }}
    >
      {getValue<string>()}
    </div>
  );
};

export default BreakableTextCell;
