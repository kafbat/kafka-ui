import React from 'react';
import IndeterminateCheckbox from 'components/common/IndeterminateCheckbox/IndeterminateCheckbox';
import { HeaderContext } from '@tanstack/react-table';

function SelectRowHeader<T>({ table }: HeaderContext<T, unknown>) {
  return (
    <IndeterminateCheckbox
      checked={table.getIsAllRowsSelected()}
      indeterminate={table.getIsSomeRowsSelected()}
      onChange={table.getToggleAllRowsSelectedHandler()}
    />
  );
}

export default SelectRowHeader;
