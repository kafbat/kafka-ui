import {
  ColumnDef,
  ColumnFilter,
  ColumnFiltersState,
} from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';
import {
  FilterableColumnDef,
  KafbatFilterVariant,
  getFilterableColumns,
} from 'components/common/NewTable/ColumnFilter';
import { useCallback, useMemo } from 'react';

import { Persister } from './types';

function getParamsByKeys(
  params: URLSearchParams,
  keyToFilterVariant: {
    [k: string]: KafbatFilterVariant;
  }
) {
  return Object.entries(keyToFilterVariant).reduce(
    (acc, [key, variant]) => {
      const foundValue = params.get(key);
      if (foundValue) {
        if (variant === 'multi-select') {
          // Array stored to query params as string, we should recover it back to array of values
          acc[key] = foundValue.split(',');
        } else {
          acc[key] = foundValue;
        }
      }
      return acc;
    },
    {} as Record<string, string | string[]>
  );
}
// By default tanstack table replace all . in accessrorKey by _
// We should normalize out filterable columns key accordingly
const normalizeAccessorKey = (accessorKey: string) =>
  accessorKey.replace(/\./g, '_');

function mapColumnKeyToFilterVariant<TData, TValue>(
  columns: FilterableColumnDef<TData, TValue>[]
): {
  [k: string]: KafbatFilterVariant;
} {
  return columns.reduce(
    (acc, cur) => {
      if (cur.meta?.filterVariant) {
        const key = normalizeAccessorKey(cur.meta.filterKey ?? cur.accessorKey);
        acc[key] = cur.meta?.filterVariant;
      }

      return acc;
    },
    {} as Record<string, KafbatFilterVariant>
  );
}

function isEmptyFilterValue(columnFilter: ColumnFilter): boolean {
  if (Array.isArray(columnFilter.value) && columnFilter.value.length === 0) {
    return true;
  }

  return columnFilter.value === undefined;
}

export function useQueryPersister<TData, TValue>(
  columns: ColumnDef<TData, TValue>[]
): Persister {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyToFilterVariant = useMemo(() => {
    const filterableColumns = getFilterableColumns(columns);

    return mapColumnKeyToFilterVariant(filterableColumns);
  }, [columns]);

  const getPrevState = useCallback(() => {
    const filterParams = getParamsByKeys(searchParams, keyToFilterVariant);
    const prevState: ColumnFiltersState = Object.entries(filterParams).map(
      ([id, value]) => {
        return { id, value };
      }
    );
    return prevState;
  }, [searchParams, keyToFilterVariant]);

  const update: Persister['update'] = useCallback(
    (nextState: ColumnFiltersState, resetPage: boolean = true) => {
      const prevState: ColumnFiltersState = getPrevState();

      const nextKeys = new Set();
      nextState.forEach((columnFilter) => {
        if (!isEmptyFilterValue(columnFilter)) {
          nextKeys.add(columnFilter.id);
          searchParams.set(columnFilter.id, String(columnFilter.value));
        }
      });

      prevState
        .map(({ id }) => id)
        .forEach((key) => {
          if (!nextKeys.has(key)) {
            searchParams.delete(key);
          }
        });

      if (resetPage) {
        searchParams.delete('page');
      }

      setSearchParams(searchParams);
    },
    [getPrevState, searchParams]
  );

  return useMemo(
    () => ({
      getPrevState,
      update,
    }),
    [getPrevState, update]
  );
}
export default useQueryPersister;
