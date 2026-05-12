import {
  ColumnDef,
  ColumnMeta,
  FilterFnOption,
  RowData,
} from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';

export type KafbatFilterVariant<T = RowData> = ColumnMeta<
  T,
  unknown
>['filterVariant'];

export type FilterableColumnDef<TData = RowData, TValue = unknown> = ColumnDef<
  TData,
  TValue
> &
  Required<{
    filterFn: FilterFnOption<FullConnectorInfo>;
    accessorKey: string;
    meta: ColumnMeta<TData, TValue> & {
      filterVariant: KafbatFilterVariant;
    };
  }>;
