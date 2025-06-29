import { ColumnDef } from '@tanstack/react-table';
import { FilterableColumnDef } from 'components/common/NewTable/ColumnFilter/types';

export function isFilterableColumn<TData, TValue>(
  column: ColumnDef<TData, TValue>
): column is FilterableColumnDef<TData, TValue> {
  return (
    'accessorKey' in column &&
    typeof column.accessorKey === 'string' &&
    !!column.meta &&
    'filterVariant' in column.meta
  );
}

export function getFilterableColumns<TData, TValue>(
  columns: ColumnDef<TData, TValue>[]
): FilterableColumnDef<TData, TValue>[] {
  return columns.filter(isFilterableColumn);
}
