import styled from 'styled-components';
import { MultiSelect as ReactMultiSelect } from 'react-multi-select-component';

const MultiSelect = styled(ReactMultiSelect)<{
  minWidth?: string;
  height?: string;
}>`
  min-width: ${({ minWidth }) => minWidth || '200px;'};
  height: ${({ height }) => height ?? '32px'};
  font-size: 14px;
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

  .options li .select-item,
  .options .select-item {
    display: flex;
    align-items: center;

    background-color: ${({ theme }) =>
      theme.select.backgroundColor.normal} !important;

    input[type='checkbox'] {
      appearance: none;
      -webkit-appearance: none;
      position: relative;

      display: flex;
      align-items: center;
    }

    .item-renderer span {
      color: ${({ theme }) => theme.select.color.normal} !important;
    }

    &::before {
      content: ' ';
      flex-shrink: 0;
      display: block;
      margin: 0 8px 0 0;
      width: 16px;
      height: 16px;
      border-radius: 3px;

      vertical-align: middle;

      border: 1px solid
        ${({ theme }) => theme.select.multiSelectOption.checkbox.borderColor} !important;
      background-color: ${({ theme }) =>
        theme.select.multiSelectOption.checkbox.backgroundColor} !important;
    }

    &:hover {
      background-color: ${({ theme }) =>
        theme.select.backgroundColor.hover} !important;

      .item-renderer span {
        color: ${({ theme }) => theme.select.color.hover} !important;
      }
    }

    &.selected {
      background-color: ${({ theme }) =>
        theme.select.backgroundColor.active} !important;

      .item-renderer span {
        color: ${({ theme }) => theme.select.color.active} !important;
      }

      &::before {
        border-width: 2px !important;
        border-radius: 4px !important;
        border-color: ${({ theme }) => theme.select.color.active} !important;
        background-color: ${({ theme }) =>
          theme.select.backgroundColor.active} !important;
      }

      input[type='checkbox']::before {
        content: ' ';
        position: absolute;
        top: -5px;
        left: -24px;

        width: 17px;
        height: 1px;
        transform: rotate(45deg);
        background-color: ${({ theme }) =>
          theme.select.color.active} !important;
      }
      input[type='checkbox']::after {
        content: ' ';
        position: absolute;
        top: -5px;
        left: -25px;

        width: 17px;
        height: 1px;
        transform: rotate(-45deg);
        background-color: ${({ theme }) =>
          theme.select.color.active} !important;
      }

      &:hover {
        background-color: ${({ theme }) =>
          theme.select.backgroundColor.hover} !important;

        .item-renderer span {
          color: ${({ theme }) => theme.select.color.hover} !important;
        }

        input[type='checkbox']::before,
        input[type='checkbox']::after {
          background-color: ${({ theme }) =>
            theme.select.color.hover} !important;
        }

        &::before {
          border-color: ${({ theme }) => theme.select.color.hover} !important;
          background-color: ${({ theme }) =>
            theme.select.backgroundColor.hover} !important;
        }
      }
    }

    &.disabled:before {
      background: #eee;
      color: #aaa;
    }
  }

  & > .dropdown-container {
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

    & > .dropdown-heading {
      height: ${({ height }) => height ?? '32px'};
      color: ${({ disabled, theme }) =>
        disabled
          ? theme.select.color.disabled
          : theme.select.color.active} !important;
      & > .clear-selected-button {
        display: none;
      }
      &:hover {
        & > .clear-selected-button {
          display: block;
        }
      }
    }
  }
`;

export default MultiSelect;
