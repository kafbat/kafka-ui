import React, { useCallback } from 'react';
import useBoolean from 'lib/hooks/useBoolean';
import { Column } from '@tanstack/react-table';
import * as FilterType from './variants';
interface FilterComponentProps<T> {
  column: Column<T, unknown>;
}
const FilterComponent = <T,>(props: FilterComponentProps<T>) => {
  const { column } = props;
  const filterVariant = column.columnDef.meta?.filterVariant;

  if (filterVariant === 'select') {
    return (
      <FilterType.MultiSelectFilter column={column} onChange={console.log} />
    );
  }

  return null;
};

/* MAIN COMPONENT */
interface FilterProps<T> {
  column: Column<T, unknown>;
}
const Filter = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  const onChange = useCallback(
    (v) => {
      column.setFilterValue(v);
    },
    [column]
  );

  return (
    <div style={{ position: 'relative' }}>
      <FilterType.MultiSelectFilter column={column} onChange={onChange} />
    </div>
  );
};

export default Filter;
