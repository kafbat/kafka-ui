import { ColumnSizingState } from '@tanstack/react-table';
import { useEffect, useMemo, useRef, useState } from 'react';

import { ColumnSizingPersister } from './types';

const STORAGE_KEY = 'kafbat_tables';

type TableColumnSizing = {
  [tableName: string]: ColumnSizingState;
};

export function useLocalStoragePersister(
  tableName?: string
): ColumnSizingPersister {
  const refAllData = useRef<TableColumnSizing | null>(null);

  const [columnSizing, setColumnSizing] = useState(() => {
    if (!tableName) {
      return {};
    }
    let sizing: ColumnSizingState = {};
    try {
      const allTablesSizing = JSON.parse(
        localStorage.getItem(STORAGE_KEY) ?? '{}'
      ) as TableColumnSizing;
      sizing = allTablesSizing[tableName] ?? {};
      refAllData.current = allTablesSizing;
    } catch (e) {
      console.error('Couldnt parse colum column sizing from local storage');
    }
    return sizing;
  });

  useEffect(() => {
    if (!tableName) {
      return;
    }
    const allTables = refAllData.current ?? {};
    refAllData.current = { ...allTables, [tableName]: columnSizing };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(refAllData.current));
  }, [columnSizing]);

  useEffect(() => {
    return () => {
      if (!tableName) {
        return;
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(refAllData.current));
    };
  }, []);

  return useMemo(() => {
    if (!tableName) {
      return {
        columnSizing: {},
        setColumnSizing: () => {},
      };
    }

    return {
      columnSizing,
      setColumnSizing,
    };
  }, [columnSizing, setColumnSizing]);
}
