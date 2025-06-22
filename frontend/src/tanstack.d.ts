/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable no-unused-vars */
import { type RowData, type FilterFn } from '@tanstack/react-table';

type KafbatFilterVariant = 'multi-select';

declare module '@tanstack/react-table' {
  interface ColumnMeta<TData extends RowData, TValue> {
    filterVariant?: KafbatFilterVariant;
    width?: string;
  }

  interface FilterFns {
    includesSome: FilterFn<unknown>;
  }
}
