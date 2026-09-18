import type { Row } from '@tanstack/react-table';

// Shared filter functions used by table instances that need filtering.
// Kept in a single module so the definitions are not duplicated across
// components (see Table.tsx and ConsumerGroups/Details/Details.tsx).
export const filterFns = {
  includesSome: (
    row: Row<unknown>,
    columnId: string,
    filterValue: string[]
  ): boolean => {
    if (filterValue.length === 0) {
      return true;
    }
    return filterValue.includes(row.getValue(columnId));
  },
  noop: (): boolean => {
    return true;
  },
};
