import React from 'react';
import { Column } from '@tanstack/react-table';

import * as FilterVariant from './variants';

interface FilterProps<T> {
  column: Column<T, unknown>;
}
export const Filter = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  switch (column.columnDef.meta?.filterVariant) {
    case 'multi-select': {
      return <FilterVariant.MultiSelect column={column} />;
    }
    default: {
      throw Error('Not implemented filter');
    }
  }
};
