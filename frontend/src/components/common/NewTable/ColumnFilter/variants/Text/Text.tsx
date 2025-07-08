import React from 'react';
import { Column } from '@tanstack/react-table';
import { FilterContainer } from 'components/common/NewTable/ColumnFilter/ui/FilterContainer/FilterContainer';
import Input from 'components/common/Input/Input';

interface FilterProps<T> {
  column: Column<T, unknown>;
}

const TextFilterInput = <T,>(props: FilterProps<T>) => {
  const { column } = props;
  const value = column.getFilterValue() as string;
  return (
    <Input
      value={value}
      onChange={({ target }) => {
        column.setFilterValue(target?.value ?? '');
      }}
    />
  );
};

export const Text = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  const value = column.getFilterValue() as string | undefined;

  return (
    <FilterContainer
      column={column}
      filterComponent={<TextFilterInput column={column} />}
      hasFilterValue={!!value?.length}
    />
  );
};
