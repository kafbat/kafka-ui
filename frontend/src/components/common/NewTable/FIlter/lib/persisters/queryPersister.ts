import { ColumnFilter, ColumnFiltersState } from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';
import {
  FilterableColumnDef,
  KafbatFilterVariant,
} from 'components/common/NewTable/Filter/types';
import { useCallback, useMemo } from 'react';

import { Persister } from './Persister';

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

function mapColumnKeyToFilterVariant<TData, TValue>(
  columns: FilterableColumnDef<TData, TValue>[]
): {
  [k: string]: KafbatFilterVariant;
} {
  return columns.reduce(
    (acc, cur) => {
      if (cur.meta?.filterVariant) {
        acc[cur.accessorKey] = cur.meta?.filterVariant;
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
  columns: FilterableColumnDef<TData, TValue>[]
): Persister {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyToFilterVariant = useMemo(
    () => mapColumnKeyToFilterVariant(columns),
    [columns]
  );

  const getPrevState = useCallback(() => {
    const filterParams = getParamsByKeys(searchParams, keyToFilterVariant);
    const prevState: ColumnFiltersState = Object.entries(filterParams).map(
      ([id, value]) => {
        return { id, value };
      }
    );
    return prevState;
  }, [searchParams, keyToFilterVariant]);

  const update = useCallback(
    (nextState: ColumnFiltersState) => {
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
