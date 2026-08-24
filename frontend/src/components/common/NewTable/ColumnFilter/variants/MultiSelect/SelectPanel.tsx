import React, { useCallback, useMemo, useState } from 'react';
import { Column } from '@tanstack/react-table';

import * as S from './MultiSelect.styled';
import { type Option } from './types';
import {
  toOption,
  getOptionValue,
  customValueRenderer,
  sortOptionSelectedFirst,
} from './lib';

interface Props<T, K = string> {
  column: Column<T, K>;
}

function SelectPanel<T, K = string>(props: Props<T, K>) {
  const { column } = props;

  const [selectedOptions, setValues] = useState<Option[]>(() => {
    const value = column.getFilterValue() as string[] | undefined;

    if (value) {
      return value.map(toOption);
    }

    return [];
  });

  const predefinedValues = column.columnDef.meta?.filterValues;
  const columnFacetedValues = column.getFacetedUniqueValues();

  const sortedOptions = useMemo(() => {
    let values = [];
    if (predefinedValues) {
      values = predefinedValues;
    } else {
      values = [...new Set([...columnFacetedValues.keys()].flat())];
    }
    const allOptions = values.map(toOption);

    return sortOptionSelectedFirst(selectedOptions, allOptions);
  }, [predefinedValues, columnFacetedValues]);

  const onSelect = useCallback((options: Option[]) => {
    column.setFilterValue(options.map(getOptionValue));
    setValues(options);
  }, []);

  return (
    <S.SelectPanel
      isOpen
      options={sortedOptions}
      value={selectedOptions}
      onChange={onSelect}
      labelledBy=""
      valueRenderer={customValueRenderer}
      hasSelectAll
    />
  );
}

export default SelectPanel;
