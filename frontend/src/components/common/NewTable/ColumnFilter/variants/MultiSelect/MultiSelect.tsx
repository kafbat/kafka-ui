import React, { useState, useRef, useMemo } from 'react';
import { Column } from '@tanstack/react-table';
import useBoolean from 'lib/hooks/useBoolean';
import Portal from 'components/common/Portal/Portal';
import useClickOutside from 'lib/hooks/useClickOutside';

import ClearIcon from './ui/ClearIcon';
import SelectPanel from './SelectPanel';
import * as S from './MultiSelect.styled';
import FilterIcon from './ui/FilterIcon';

interface FilterProps<T> {
  column: Column<T, unknown>;
}

const TOP_PADDING = 8;
const LEFT_PADDING = 16;

export const MultiSelect = <T,>(props: FilterProps<T>) => {
  const { column } = props;
  const { value: opened, toggle } = useBoolean(false);
  const [coords, setCoords] = useState<{ left: number; top: number }>({
    left: 0,
    top: 0,
  });

  const ref = useRef(null);
  useClickOutside(ref, toggle);

  const resetFilter = () => column.setFilterValue('');
  const onFilterClick = (
    event: React.MouseEvent<HTMLDivElement, MouseEvent>
  ) => {
    const node = event.target as HTMLElement;
    const rect = node.getBoundingClientRect();
    setCoords({
      left: rect.left + LEFT_PADDING,
      top: rect.bottom + TOP_PADDING,
    });
    toggle();
  };

  const value: string[] = useMemo(() => {
    const filterValue = column.getFilterValue();
    if (filterValue && Array.isArray(filterValue)) {
      return filterValue;
    }

    return [];
  }, [column.getFilterValue()]);

  return (
    <S.Container>
      <S.FilterIcon onClick={onFilterClick}>
        <FilterIcon active={opened || !!value.length} />
      </S.FilterIcon>

      {!!value.length && (
        <>
          <S.Count>{value.length}</S.Count>
          <S.ResetIcon onClick={resetFilter}>
            <ClearIcon />
          </S.ResetIcon>
        </>
      )}

      <Portal isOpen={opened}>
        <S.Positioner
          ref={ref}
          style={{
            left: coords.left,
            top: coords.top,
          }}
        >
          <SelectPanel column={column} />
        </S.Positioner>
      </Portal>
    </S.Container>
  );
};
