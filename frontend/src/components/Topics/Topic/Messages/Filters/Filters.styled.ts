import Input from 'components/common/Input/Input';
import Select from 'components/common/Select/Select';
import styled, { css } from 'styled-components';
import DatePicker from 'react-datepicker';
import EditIcon from 'components/common/Icons/EditIcon';
import closeIcon from 'components/common/Icons/CloseIcon';
import { PollingMode } from 'generated-sources';

import { isModeOptionWithInput } from './utils';

interface SavedFilterProps {
  selected: boolean;
}
interface MessageLoadingProps {
  isLive: boolean;
}

interface MessageLoadingSpinnerProps {
  isFetching: boolean;
}

export const FilterModeTypeSelectorWrapper = styled.div`
  display: flex;
  & .select-wrapper {
    & > select {
      border-radius: 4px 0 0 4px !important;
    }
  }
`;

export const OffsetSelector = styled(Input)`
  border-radius: 0 4px 4px 0 !important;
  &::placeholder {
    color: ${({ theme }) => theme.input.color.normal};
  }
`;

export const DatePickerInput = styled(DatePicker)<{ $fixedWidth?: boolean }>`
  height: 32px;
  border: 1px ${({ theme }) => theme.select.borderColor.normal} solid;
  border-left: none;
  border-radius: 0 4px 4px 0;
  font-size: 14px;
  width: ${({ $fixedWidth }) => ($fixedWidth ? '160px' : '100%')};
  min-width: 160px;
  padding-left: 12px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  color: ${({ theme }) => theme.input.color.normal};
  &::placeholder {
    color: ${({ theme }) => theme.input.color.normal};
  }

  background-image: url('data:image/svg+xml,%3Csvg width="10" height="6" viewBox="0 0 10 6" fill="none" xmlns="http://www.w3.org/2000/svg"%3E%3Cpath d="M1 1L5 5L9 1" stroke="%23454F54"/%3E%3C/svg%3E%0A') !important;
  background-repeat: no-repeat !important;
  background-position-x: 96% !important;
  background-position-y: 55% !important;
  appearance: none !important;

  &:hover {
    cursor: pointer;
  }
  &:focus {
    outline: none;
  }
`;

export const DatePickerRangeInput = styled(DatePicker)`
  height: 32px;
  border: 1px ${({ theme }) => theme.select.borderColor.normal} solid;
  border-left: none;
  border-radius: 0;
  font-size: 14px;
  width: 160px;
  min-width: 160px;
  padding-left: 12px;
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  color: ${({ theme }) => theme.input.color.normal};
  &::placeholder {
    color: ${({ theme }) => theme.input.color.normal};
  }

  background-image: url('data:image/svg+xml,%3Csvg width="10" height="6" viewBox="0 0 10 6" fill="none" xmlns="http://www.w3.org/2000/svg"%3E%3Cpath d="M1 1L5 5L9 1" stroke="%23454F54"/%3E%3C/svg%3E%0A') !important;
  background-repeat: no-repeat !important;
  background-position-x: 96% !important;
  background-position-y: 55% !important;
  appearance: none !important;

  &:hover {
    cursor: pointer;
  }
  &:focus {
    outline: none;
  }

  &:last-of-type {
    border-radius: 0 4px 4px 0;
  }
`;

export const DateRangeSeparator = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.input.color.normal};
  background-color: ${({ theme }) => theme.input.backgroundColor.normal};
  border-top: 1px ${({ theme }) => theme.select.borderColor.normal} solid;
  border-bottom: 1px ${({ theme }) => theme.select.borderColor.normal} solid;
`;

export const Message = styled.div`
  font-size: 14px;
  color: ${({ theme }) => theme.metrics.filters.color.normal};
`;
export const Metric = styled.div`
  color: ${({ theme }) => theme.metrics.filters.color.normal};
  font-size: 12px;
  display: flex;
`;

export const MetricsIcon = styled.div`
  color: ${({ theme }) => theme.metrics.filters.color.icon};
  padding-right: 6px;
  height: 12px;
`;
export const ListItem = styled.li`
  font-size: 12px;
  font-weight: 400;
  margin: 4px 0;
  line-height: 1.5;
  color: ${({ theme }) => theme.table.td.color.normal};
`;

export const InfoHeading = styled.div`
  font-size: 16px;
  font-weight: 500;
  line-height: 1.5;
  margin-bottom: 10px;
  color: ${({ theme }) => theme.table.td.color.normal};
`;

export const InfoParagraph = styled.div`
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
  margin-bottom: 10px;
  color: ${({ theme }) => theme.table.td.color.normal};
`;

export const InfoModal = styled.div`
  height: auto;
  width: 560px;
  border-radius: 8px;
  background: ${({ theme }) => theme.modal.backgroundColor};
  position: absolute;
  left: 25%;
  border: 1px solid ${({ theme }) => theme.modal.border.contrast};
  box-shadow: ${({ theme }) => theme.modal.shadow};
  padding: 32px;
  z-index: 1;
`;

export const QuestionIconContainer = styled.button`
  cursor: pointer;
  padding: 0;
  background: none;
  border: none;
`;

export const NoSavedFilter = styled.p`
  color: ${({ theme }) => theme.default.color.normal};
  font-size: 16px;
  margin-top: 10px;
`;
export const SavedFiltersContainer = styled.div`
  overflow-y: auto;
  height: 195px;
  display: flex;
  flex-direction: column;
`;

export const SavedFilterName = styled.div`
  font-size: 14px;
  line-height: 20px;
  color: ${({ theme }) => theme.savedFilter.filterName};
`;

export const FilterButtonWrapper = styled.div<{ isEdit: boolean }>`
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  gap: 10px;
  padding-top: 16px;
  position: relative;
  &:before {
    content: '';
    width: calc(100% + 32px);
    height: 1px;
    position: absolute;
    top: 0;
    left: -16px;
    display: inline-block;
    background-color: ${({ theme }) => theme.modal.border.bottom};
  }
`;

export const DeleteSavedFilter = styled.button`
  cursor: pointer;
  color: ${({ theme }) => theme.icons.deleteIcon};
  background-color: transparent;
  border: none;
`;

export const FilterEdit = styled.button`
  font-weight: 500;
  font-size: 14px;
  line-height: 20px;
  background-color: transparent;
  border: none;
  cursor: pointer;
`;

export const FilterOptions = styled.div`
  display: none;
  width: 50px;
  justify-content: space-between;
  color: ${({ theme }) => theme.editFilter.textColor};
`;

export const SavedFilter = styled.div.attrs({
  role: 'savedFilter',
})<SavedFilterProps>`
  display: flex;
  justify-content: space-between;
  padding: 5px;
  height: 32px;
  border-radius: 4px;
  align-items: center;
  cursor: pointer;
  &:hover ${FilterOptions} {
    display: flex;
  }
  &:hover {
    background-color: ${({ theme }) => theme.layout.stuffColor};
  }

  background-color: ${({ selected, theme }) =>
    selected ? theme.layout.stuffColor : 'transparent'};
`;

export const ActiveSmartFilter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 32px;
  color: ${({ theme }) => theme.activeFilter.color};
  background: ${({ theme }) => theme.activeFilter.backgroundColor};
  border-radius: 4px;
  font-size: 14px;
  line-height: 20px;
`;

export const EditSmartFilterIcon = styled.button(
  ({ theme: { icons } }) => css`
    color: ${icons.editIcon.normal};
    display: flex;
    align-items: center;
    justify-content: center;
    height: 32px;
    width: 32px;
    cursor: pointer;
    background-color: transparent;
    border: none;
    border-left: 1px solid ${icons.editIcon.border};

    &:hover:not(:disabled) {
      ${EditIcon} {
        fill: ${icons.editIcon.hover};
      }
    }

    &:active:not(:disabled) {
      ${EditIcon} {
        fill: ${icons.editIcon.active};
      }
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `
);

export const SmartFilterName = styled.div`
  padding: 0 8px;
  min-width: 32px;
`;

export const DeleteSmartFilterIcon = styled.button(
  ({ theme: { icons } }) => css`
    color: ${icons.closeIcon.normal};
    display: flex;
    align-items: center;
    justify-content: center;
    height: 32px;
    width: 32px;
    cursor: pointer;
    background-color: transparent;
    border: none;
    border-left: 1px solid ${icons.closeIcon.border};

    svg {
      height: 14px;
      width: 14px;
    }

    &:hover:not(:disabled) {
      ${closeIcon} {
        fill: ${icons.closeIcon.hover};
      }
    }

    &:active:not(:disabled) {
      ${closeIcon} {
        fill: ${icons.closeIcon.active};
      }
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
    }
  `
);

export const MessageLoading = styled.div.attrs({
  role: 'contentLoader',
})<MessageLoadingProps>`
  color: ${({ theme }) => theme.heading.h3.color};
  font-size: ${({ theme }) => theme.heading.h3.fontSize};
  display: ${({ isLive }) => (isLive ? 'flex' : 'none')};
  justify-content: space-around;
  width: 260px;
`;

export const StopLoading = styled.button`
  color: ${({ theme }) => theme.heading.base.color};
  font-size: ${({ theme }) => theme.heading.h3.fontSize};
  cursor: pointer;
  background-color: transparent;
  border: none;
`;

export const MessageLoadingSpinner = styled.div<MessageLoadingSpinnerProps>`
  display: ${({ isFetching }) => (isFetching ? 'block' : 'none')};
  border: 3px solid ${({ theme }) => theme.pageLoader.borderColor};
  border-bottom: 3px solid ${({ theme }) => theme.pageLoader.borderBottomColor};
  border-radius: 50%;
  width: 20px;
  height: 20px;
  animation: spin 1.3s linear infinite;

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
`;

// styled component lib bug it does not pick up the generic
export const FilterModeTypeSelect = styled(Select<PollingMode>)`
  border-top-right-radius: ${(props) =>
    !props.value || !isModeOptionWithInput(props.value) ? '4px' : '0'};
  border-bottom-right-radius: ${(props) =>
    !props.value || !isModeOptionWithInput(props.value) ? '4px' : '0'};
  user-select: none;
`;

export const SavedFilterText = styled.div`
  font-weight: 600;
  color: ${({ theme }) => theme.default.color.normal};
`;

export const SavedFilterClearAll = styled.button`
  font-weight: 500;
  color: ${({ theme }) => theme.link.color};
  background-color: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;
