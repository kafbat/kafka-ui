import styled from 'styled-components';
import { MultiSelect as ReactMultiSelect } from 'react-multi-select-component';

export const MultiSelect = styled(ReactMultiSelect)<{
  minWidth?: string;
  height?: string;
}>`
  font-size: 14px;
  padding-right: 12px;
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

    height: ${({ height }) => height ?? '32px'};
    * {
      cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
    }

    & > .dropdown-content {
      width: fit-content;
      min-width: 120px;
      right: 0px;
    }

    & > .dropdown-heading {
      height: ${({ height }) => height ?? '32px'};
      color: ${({ disabled, theme }) =>
        disabled
          ? theme.select.color.disabled
          : theme.select.color.active} !important;
      & > .dropdown-heading-value {
        color: ${({ theme }) => theme.table.filter.multiSelect.value.color};
      }
    }
  }

  & .clear-selected-button + div {
    color: ${({ theme }) =>
      theme.table.filter.multiSelect.filterIcon.fill.active};
  }
`;
