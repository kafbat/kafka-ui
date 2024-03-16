import React, { FC } from 'react';
import DeleteIcon from 'components/common/Icons/DeleteIcon';
import { useConfirm } from 'lib/hooks/useConfirm';
import {
  AdvancedFilter,
  useMessageFiltersStore,
} from 'lib/hooks/useMessageFiltersStore';
import EditIcon from 'components/common/Icons/EditIcon';
import Flexbox from 'components/common/FlexBox/FlexBox';

import * as S from './Filters.styled';

export interface Props {
  filters: Record<string, AdvancedFilter>;
  smartFilter?: AdvancedFilter;
  setSmartFilter: (filter: AdvancedFilter | null) => void;
  onEdit: (id: string) => void;
  closeSideBar: () => void;
}

const SavedFilters: FC<Props> = ({
  filters,
  closeSideBar,
  smartFilter,
  setSmartFilter,
  onEdit,
}) => {
  const filtersList = React.useMemo(() => Object.values(filters), [filters]);
  const clearAll = useMessageFiltersStore((state) => state.removeAll);
  const remove = useMessageFiltersStore((state) => state.remove);
  const confirm = useConfirm();

  const activateFilter = (filter: AdvancedFilter) => {
    setSmartFilter(filter);
    closeSideBar();
  };

  const deleteFilterHandler = (id: string) => {
    const isFilterSelected = smartFilter && smartFilter.id === id;

    confirm(
      <>
        <p>Are you sure want to remove {id}?</p>
        {isFilterSelected && (
          <>
            <br />
            <p>Warning: this filter is currently selected.</p>
          </>
        )}
      </>,
      () => {
        setSmartFilter(null);
        remove(id);
      }
    );
  };

  return (
    <>
      <Flexbox margin="10px 0 0 0" justifyContent="space-between">
        <S.SavedFilterText>Saved Filters</S.SavedFilterText>
        <S.SavedFilterClearAll
          onClick={clearAll}
          disabled={filtersList.length === 0}
        >
          Clear all
        </S.SavedFilterClearAll>
      </Flexbox>
      <S.SavedFiltersContainer>
        {filtersList.length === 0 && (
          <S.NoSavedFilter>No saved filter(s)</S.NoSavedFilter>
        )}
        {filtersList.map((filter) => (
          <S.SavedFilter
            key={Symbol(filter.id).toString()}
            selected={smartFilter?.id === filter.id}
            onClick={() => activateFilter(filter)}
          >
            <S.SavedFilterName>{filter.id}</S.SavedFilterName>
            <S.FilterOptions>
              <S.FilterEdit
                aria-label="edit"
                onClick={(event) => {
                  event.stopPropagation();
                  onEdit(filter.id);
                }}
              >
                <EditIcon />
              </S.FilterEdit>
              <S.DeleteSavedFilter
                aria-label="delete"
                onClick={(event) => {
                  event.stopPropagation();
                  deleteFilterHandler(filter.id);
                }}
              >
                <DeleteIcon />
              </S.DeleteSavedFilter>
            </S.FilterOptions>
          </S.SavedFilter>
        ))}
      </S.SavedFiltersContainer>
    </>
  );
};

export default SavedFilters;
