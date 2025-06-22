import { ColumnDef, ColumnFiltersState } from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';
import { KafbatFilterVariant } from 'tanstack';

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

function mapColumnKeyToFilterVariant<T>(columns: ColumnDef<T, unknown>[]): {
  [k: string]: KafbatFilterVariant;
} {
  return columns.reduce(
    (acc, cur) => {
      if (cur.meta?.filterVariant && cur.accessorKey) {
        acc[cur.accessorKey] = cur.meta.filterVariant;
      }

      return acc;
    },
    {} as Record<string, KafbatFilterVariant>
  );
}

export interface Persister {
  getPrevState: () => ColumnFiltersState;
  store: (nextState: ColumnFiltersState) => void;
}

function useQueryParamsPersister<T>(columns: ColumnDef<T>[]) {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyToFilterVariant = mapColumnKeyToFilterVariant(columns);

  function getPrevState(): ColumnFiltersState {
    const filterParams = getParamsByKeys(searchParams, keyToFilterVariant);
    const prevState: ColumnFiltersState = Object.entries(filterParams).map(
      ([id, value]) => {
        return { id, value };
      }
    );
    return prevState;
  }

  function store(nextState: ColumnFiltersState): void {
    const prevState: ColumnFiltersState = getPrevState();

    const nextKeys = new Set();

    nextState.forEach((columnFilter) => {
      nextKeys.add(columnFilter.id);
      searchParams.set(columnFilter.id, String(columnFilter.value));
    });

    prevState
      .map((cf) => cf.id)
      .forEach((key) => {
        if (!nextKeys.has(key)) {
          searchParams.delete(key);
        }
      });

    setSearchParams(searchParams);
  }

  return {
    getPrevState,
    store,
  };
}
export default useQueryParamsPersister;
