import styled from 'styled-components';
import { MultiSelect as ReactMultiSelect } from 'react-multi-select-component';

export const SelectPanel = styled(ReactMultiSelect)<{
  minWidth?: string;
  height?: string;
}>`
  font-size: 14px;
  padding-right: 12px;
  .dropdown-container:focus-within {
    box-shadow: none !important;
    border-color: none !important;
  }
  .search input {
    color: ${({ theme }) => theme.input.color.normal};
    background-color: ${(props) =>
      props.theme.input.backgroundColor.normal} !important;
  }
  .select-item {
    color: ${({ theme }) => theme.select.color.normal};
    background-color: ${({ theme }) =>
      theme.select.backgroundColor.normal} !important;

    &:active {
      background-color: ${({ theme }) =>
        theme.select.backgroundColor.active} !important;
    }
  }
  .select-item.selected {
    background-color: ${({ theme }) =>
      theme.select.backgroundColor.active} !important;
  }
  .options li {
    background-color: ${({ theme }) =>
      theme.select.backgroundColor.normal} !important;
  }

  & > .dropdown-container {
    border: none !important;
    background-color: ${({ theme }) =>
      theme.input.backgroundColor.normal} !important;
    border-color: ${({ theme }) => theme.select.borderColor.normal} !important;
    &:hover {
      border-color: ${({ theme }) => theme.select.borderColor.hover} !important;
    }

    * {
      cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
    }

    & > .dropdown-content {
      width: fit-content;
      min-width: 120px;
      right: 0px;
      top: 0;
      padding-top: 0;
    }

    & > .dropdown-heading {
      display: none;
    }
  }

  & .clear-selected-button + div {
    color: ${({ theme }) =>
      theme.table.filter.multiSelect.filterIcon.fill.active};
  }
`;

export const Container = styled.div`
  display: flex;
  height: 24px;
  align-items: center;
  position: relative;
  padding-right: 8px;
`;

export const Positioner = styled.div`
  position: absolute;
  z-index: 30;
`;

export const Count = styled.span`
  padding-left: 4px;
  color: ${({ theme }) => theme.table.filter.multiSelect.value.color};
`;

export const FilterIcon = styled.div`
  height: 12px;
  margin: 2px;
`;

export const ResetIcon = styled.div`
  margin: 3px;
`;
