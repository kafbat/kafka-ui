import React from 'react';
import { Column } from '@tanstack/react-table';

import * as Variant from './variants';

interface FilterProps<T> {
  column: Column<T, unknown>;
}

export const ColumnFilter = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  switch (column.columnDef.meta?.filterVariant) {
    case 'multi-select': {
      return <Variant.MultiSelect column={column} />;
    }
    case 'text': {
      return <Variant.Text column={column} />;
    }
    default: {
      throw Error('Not implemented filter');
    }
  }
};
