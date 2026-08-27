import React, { Suspense } from 'react';
import SlidingSidebar from 'components/common/SlidingSidebar';
import PageLoader from 'components/common/PageLoader/PageLoader';
import {
  AdvancedFilter,
  selectFilter,
  useMessageFiltersStore,
} from 'lib/hooks/useMessageFiltersStore';

import AddEditFilterContainer from './AddEditFilterContainer';
import SavedFilters from './SavedFilters';
import { isEditingFilterMode } from './utils';

export interface FilterModalProps {
  setClose: () => void;
  filterName?: string;
  setFilterName: (filterId: string) => void;
  smartFilter?: AdvancedFilter;
  setSmartFilter: (filter: AdvancedFilter | null) => void;
}

const FiltersSideBar: React.FC<FilterModalProps> = ({
  setClose,
  filterName,
  setFilterName,
  smartFilter,
  setSmartFilter,
}) => {
  const filters = useMessageFiltersStore((state) => state.filters);
  const filter = useMessageFiltersStore(selectFilter(filterName));
  const isEditing = isEditingFilterMode(filterName);

  return (
    <SlidingSidebar
      open={!!filterName}
      onClose={setClose}
      title={isEditing ? 'Edit Filter' : 'Add Filter'}
    >
      <Suspense fallback={<PageLoader />}>
        <AddEditFilterContainer
          setSmartFilter={setSmartFilter}
          closeSideBar={setClose}
          currentFilter={isEditing && filter ? filter : undefined}
          smartFilter={smartFilter}
          key={filterName}
        />

        {!isEditing && (
          <SavedFilters
            filters={filters}
            onEdit={(name) => {
              setFilterName(name);
            }}
            closeSideBar={setClose}
            smartFilter={smartFilter}
            setSmartFilter={setSmartFilter}
          />
        )}
      </Suspense>
    </SlidingSidebar>
  );
};

export default FiltersSideBar;
