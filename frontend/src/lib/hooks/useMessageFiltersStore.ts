import { LOCAL_STORAGE_KEY_PREFIX } from 'lib/constants';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type AdvancedFiltersType = Record<string, AdvancedFilter>;

export interface AdvancedFilter {
  id: string;
  value: string;
  filterCode: string;
}

interface MessageFiltersState {
  filters: AdvancedFiltersType;
  save: (filter: AdvancedFilter) => void;
  nextCursor: string | undefined;
  setNextCursor: (str: string | undefined) => void;
  replace: (filterId: string, filter: AdvancedFilter) => void;
  remove: (id: string) => void;
  removeAll: () => void;
}

export const selectFilter =
  (id?: string) =>
  ({ filters }: MessageFiltersState) => {
    if (!id) return undefined;

    if (filters[id]) return filters[id];
    return undefined;
  };

export const useMessageFiltersStore = create<MessageFiltersState>()(
  persist(
    (set) => ({
      filters: {},
      nextCursor: undefined,
      notPersistedFilter: undefined,
      save: (filter) =>
        set((state) => ({
          filters: { ...state.filters, [filter.id]: filter },
        })),
      replace: (filterId, filter) =>
        set((state) => {
          const newFilters = { ...state.filters };

          if (filterId !== filter.id) {
            delete newFilters[filterId];
          }

          newFilters[filter.id] = filter;

          return { filters: newFilters };
        }),
      remove: (id) =>
        set((state) => {
          const filters = { ...state.filters };
          delete filters[id];

          return { filters };
        }),
      removeAll: () => set(() => ({ filters: {} })),
      setNextCursor: (cursor) => set(() => ({ nextCursor: cursor })),
    }),
    {
      name: `${LOCAL_STORAGE_KEY_PREFIX}-message-filters`,
      partialize: (state) => ({ filters: state.filters }),
    }
  )
);
