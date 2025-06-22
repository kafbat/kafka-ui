import React from 'react';
import { Column } from '@tanstack/react-table';

import * as FilterType from './variants';

/* MAIN COMPONENT */
interface FilterProps<T> {
  column: Column<T, unknown>;
}
const Filter = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  switch (column.columnDef.meta?.filterVariant) {
    case 'multi-select': {
      return <FilterType.MultiSelectFilter column={column} />;
    }
    default: {
      throw Error('Not implemented filter');
    }
  }
};

export default Filter;
