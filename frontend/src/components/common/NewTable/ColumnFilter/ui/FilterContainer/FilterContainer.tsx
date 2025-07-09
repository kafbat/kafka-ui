import React, { useState, useRef, ReactNode } from 'react';
import { Column } from '@tanstack/react-table';
import useBoolean from 'lib/hooks/useBoolean';
import Portal from 'components/common/Portal/Portal';
import useClickOutside from 'lib/hooks/useClickOutside';

import FilterIcon from './FilterIcon';
import ClearIcon from './ClearIcon';
import * as S from './FilterContainer.styled';

interface Props<T> {
  column: Column<T, unknown>;
  hasFilterValue: boolean;
  valueComponent?: ReactNode;
  filterComponent?: ReactNode;
}

const TOP_PADDING = 8;

export const FilterContainer = <T,>(props: Props<T>) => {
  const { column, valueComponent, filterComponent, hasFilterValue } = props;
  const { value: opened, toggle } = useBoolean(false);
  const [coords, setCoords] = useState<{ right: number; top: number }>({
    right: 0,
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
      right: window.innerWidth - rect.right,
      top: rect.bottom + TOP_PADDING,
    });
    toggle();
  };

  return (
    <S.Container>
      <S.FilterIcon onClick={onFilterClick}>
        <FilterIcon active={opened || hasFilterValue} />
      </S.FilterIcon>

      {hasFilterValue && (
        <>
          {valueComponent}
          <S.ResetIcon onClick={resetFilter}>
            <ClearIcon />
          </S.ResetIcon>
        </>
      )}

      <Portal isOpen={opened}>
        <S.Positioner
          ref={ref}
          style={{
            right: coords.right,
            top: coords.top,
          }}
        >
          {filterComponent}
        </S.Positioner>
      </Portal>
    </S.Container>
  );
};
