import React, { useCallback, useState } from 'react';
import { Column } from '@tanstack/react-table';

import * as S from './MultiSelect.styled';
import { type Option } from './types';
import {
  toOption,
  getOptionValue,
  customValueRenderer,
  sortOptionSelectedFirst,
} from './lib';
import FilterIcon from './ui/FilterIcon';
import ClearIcon from './ui/ClearIcon';

interface Props<T, K = string> {
  column: Column<T, K>;
}

function MultiSelect<T, K = string>(props: Props<T, K>) {
  const { column } = props;

  const allColumnValues = [...column.getFacetedUniqueValues().keys()].flat();
  const allOptions = allColumnValues.map(toOption);

  const [selectedOptions, setValues] = useState<Option[]>(() => {
    const value = column.getFilterValue() as string[] | undefined;

    if (value) {
      return value.map(toOption);
    }

    return [];
  });

  const sortedOptions = sortOptionSelectedFirst(selectedOptions, allOptions);

  const onSelect = useCallback((options: Option[]) => {
    column.setFilterValue(options.map(getOptionValue));
    setValues(options);
  }, []);

  return (
    <S.MultiSelect
      options={sortedOptions}
      value={selectedOptions}
      onChange={onSelect}
      labelledBy=""
      valueRenderer={customValueRenderer}
      ArrowRenderer={FilterIcon}
      hasSelectAll={false}
      ClearSelectedIcon={<ClearIcon />}
    />
  );
}

export default MultiSelect;
