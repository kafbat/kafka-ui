import { ColumnFiltersState } from '@tanstack/react-table';

export interface Persister {
  getPrevState: () => ColumnFiltersState;
  update: (nextState: ColumnFiltersState, resetPagination?: boolean) => void;
}
