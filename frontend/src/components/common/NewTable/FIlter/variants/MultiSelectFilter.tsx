import { Column } from '@tanstack/react-table';
import FilterIcon from 'components/common/Icons/FilterIcon';
import React, { useCallback, useState } from 'react';
import { MultiSelect } from './MultiSelectFilter.styled';

interface MultiSelectFilterProps<T, K = string> {
  column: Column<T, K>;
  onChange: (options: K[]) => void;
}

const customValueRenderer = (selected: { label: string; value: string }[]) => {
  return selected.length ? selected.length : ' ';
};

function CustomArrow() {
  return <FilterIcon />;
}

function MultiSelectFilter<T, K = string>(props: MultiSelectFilterProps<T, K>) {
  const { column, onChange } = props;

  const values = [...column.getFacetedUniqueValues().keys()];
  const options = values.map((v) => ({ label: v, value: v }));
  const [i, setI] = useState<{ label: string; value: string }[]>([]);

  const onSelect = useCallback((x: { label: string; value: string }[]) => {
    onChange(x.map((v) => v.value as K));
    setI(x);
  }, []);

  return (
    <div>
      <MultiSelect
        options={options}
        value={i}
        onChange={onSelect}
        labelledBy="x"
        valueRenderer={customValueRenderer}
        ArrowRenderer={CustomArrow}
        hasSelectAll={false}
      />
    </div>
  );
}

export default MultiSelectFilter;
