import { createContext, useContext, useMemo } from 'react';
import type { Table } from '@tanstack/react-table';

export type TableContextValue<T> = {
  table: Table<T> | null;
  setTable: (t: Table<T>) => void;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const TableContext = createContext<TableContextValue<any> | null>(null);

export const useTableInstance = <D>() => {
  const ctx = useContext<TableContextValue<D> | null>(TableContext);

  return useMemo(() => ctx, [ctx]);
};
