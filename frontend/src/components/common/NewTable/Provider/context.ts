import { createContext, useContext, useMemo, Context } from 'react';
import type { Table } from '@tanstack/react-table';

export type TableContextValue<T> = {
  table: Table<T> | null;
  setTable: (t: Table<T>) => void;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const TableContext = createContext<TableContextValue<any> | null>(null);

export const useTableInstance = <D>() => {
  const ctx = useContext<TableContextValue<D>>(
    TableContext as Context<TableContextValue<D>>
  );

  if (ctx === null) {
    throw new Error('useTableInstance must be used  within a provider');
  }

  return useMemo(() => ctx, [ctx]);
};
