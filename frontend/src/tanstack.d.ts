/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable no-unused-vars */
import { type RowData, type FilterFn } from '@tanstack/react-table';

declare module '@tanstack/react-table' {
  interface ColumnMeta<TData extends RowData, TValue> {
    filterVariant?: 'multi-select' | 'text';
    width?: string;
    csv?: string;
    csvFn?: (row: TData) => string;
  }

  interface FilterFns {
    includesSome: FilterFn<unknown>;
  }
}
