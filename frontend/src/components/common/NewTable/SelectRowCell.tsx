import { CellContext } from '@tanstack/react-table';
import React from 'react';
import IndeterminateCheckbox from 'components/common/IndeterminateCheckbox/IndeterminateCheckbox';

function SelectRowCell<T, V = unknown>({ row }: CellContext<T, V>) {
  return (
    <IndeterminateCheckbox
      checked={row.getIsSelected()}
      disabled={!row.getCanSelect()}
      indeterminate={row.getIsSomeSelected()}
      onChange={row.getToggleSelectedHandler()}
    />
  );
}

export default SelectRowCell;
