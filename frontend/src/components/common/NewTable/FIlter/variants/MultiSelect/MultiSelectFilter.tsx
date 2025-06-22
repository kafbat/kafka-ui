import React, { useCallback, useState } from 'react';
import { Column } from '@tanstack/react-table';

import { MultiSelect } from './MultiSelectFilter.styled';
import { type Option } from './types';
import toOption from './lib/toOption';
import getOptionValue from './lib/getOptionValue';
import CustomArrow from './ui/CustomArrow';
import customValueRenderer from './lib/customValueRenderer';

interface MultiSelectFilterProps<T, K = string> {
  column: Column<T, K>;
}

function MultiSelectFilter<T, K = string>(props: MultiSelectFilterProps<T, K>) {
  const { column } = props;

  const values = [...column.getFacetedUniqueValues().keys()].flat();
  const options = values.map(toOption);

  const [predefinedValue, setValues] = useState<Option[]>(() => {
    const value = column.getFilterValue() as string[] | undefined;

    if (value) {
      return value.map(toOption);
    }

    return [];
  });

  const onSelect = useCallback((options: Option[]) => {
    column.setFilterValue(options.map(getOptionValue));
    setValues(options);
  }, []);

  return (
    <div>
      <MultiSelect
        options={options}
        value={predefinedValue}
        onChange={onSelect}
        labelledBy=""
        valueRenderer={customValueRenderer}
        ArrowRenderer={CustomArrow}
        hasSelectAll={false}
      />
    </div>
  );
}

export default MultiSelectFilter;
