import React, { useMemo, useState } from 'react';
import type { Table } from '@tanstack/react-table';

import { TableContext, TableContextValue } from './context';

type TableProviderProps<T> = {
  children: React.ReactNode | ((ctx: TableContextValue<T>) => React.ReactNode);
};

export function TableProvider<T>({ children }: TableProviderProps<T>) {
  const [table, setTable] = useState<Table<T> | null>(null);

  const value = useMemo(() => ({ table, setTable }), [table, setTable]);

  return (
    <TableContext.Provider value={value}>
      {typeof children === 'function' ? children(value) : children}
    </TableContext.Provider>
  );
}
