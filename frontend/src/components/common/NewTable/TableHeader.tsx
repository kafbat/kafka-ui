import React, { FC } from 'react';
import * as S from './Table.styled';
import { Header, flexRender } from '@tanstack/react-table';
import FilterIcon from '../Icons/FilterIcon';
import Filter from './FIlter/Filter';

interface TableHeaderProps<TData> {
  header: Header<TData, unknown>;
}

const TableHeader = <T,>(props: TableHeaderProps<T>) => {
  const { header } = props;
  console.log('filter', header.column.getFilterValue());

  return (
    <S.Th
      key={header.id}
      colSpan={header.colSpan}
      sortable={header.column.getCanSort()}
      sortOrder={header.column.getIsSorted()}
      style={{
        width:
          header.column.getSize() !== 150 ? header.column.getSize() : undefined,
      }}
    >
      <S.TableHeaderContent>
        <div onClick={header.column.getToggleSortingHandler()}>
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
