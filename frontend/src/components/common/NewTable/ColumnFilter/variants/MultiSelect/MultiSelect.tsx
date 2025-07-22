import React, { useMemo } from 'react';
import { Column } from '@tanstack/react-table';
import { FilterContainer } from 'components/common/NewTable/ColumnFilter/ui/FilterContainer/FilterContainer';

import SelectPanel from './SelectPanel';
import * as S from './MultiSelect.styled';

interface FilterProps<T> {
  column: Column<T, unknown>;
}

export const MultiSelect = <T,>(props: FilterProps<T>) => {
  const { column } = props;

  const value: string[] = useMemo(() => {
    const filterValue = column.getFilterValue();
    if (filterValue && Array.isArray(filterValue)) {
      return filterValue;
    }

    return [];
  }, [column.getFilterValue()]);

  return (
    <FilterContainer
      column={column}
      hasFilterValue={value.length > 0}
      valueComponent={<S.Count>{value.length}</S.Count>}
      filterComponent={<SelectPanel column={column} />}
    />
  );
};
