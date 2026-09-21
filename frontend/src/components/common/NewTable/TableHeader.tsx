import React from 'react';
import { Header, flexRender } from '@tanstack/react-table';

import * as S from './Table.styled';
import Filter from './ColumnFilter';

interface TableHeaderProps<TData> {
  header: Header<TData, unknown>;
}

const TableHeader = <T,>(props: TableHeaderProps<T>) => {
  const { header } = props;

  return (
    <S.Th
      key={header.id}
      colSpan={header.colSpan}
      $sortable={header.column.getCanSort()}
      $sortOrder={header.column.getIsSorted()}
    >
      <S.TableHeaderContent>
        <div
          role="button"
          tabIndex={0}
          onClick={header.column.getToggleSortingHandler()}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              header.column.getToggleSortingHandler()?.(e);
            }
          }}
        >
          {flexRender(header.column.columnDef.header, header.getContext())}
        </div>

        {header.column.getCanFilter() &&
          header.column.columnDef.meta?.filterVariant && (
            <Filter column={header.column} />
          )}
      </S.TableHeaderContent>
    </S.Th>
  );
};

export default TableHeader;
