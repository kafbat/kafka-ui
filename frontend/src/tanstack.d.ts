/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable no-unused-vars */
import { type RowData, type FilterFn } from '@tanstack/react-table';

declare module '@tanstack/react-table' {
  interface ColumnMeta<TData extends RowData, TValue> {
    filterVariant?: 'multi-select' | 'text';
    filterKey?: string; // To align column id and accessorKey
    filterValues?: unknown[];
    width?: string;
    csv?: string;
    csvFn?: (row: TData) => string;
  }

  interface FilterFns {
    includesSome: FilterFn<unknown>;
    noop: FilterFn<unknown>; // Can be used to disable client side filtering
  }
}
