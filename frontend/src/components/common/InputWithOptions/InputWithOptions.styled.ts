import styled, { css } from 'styled-components';

export interface InputProps {
  inputSize?: 'S' | 'M' | 'L';
}

const INPUT_SIZES = {
  S: 24,
  M: 32,
  L: 40,
};

interface OptionProps {
  disabled?: boolean;
}

export const Wrapper = styled.div<InputProps>(
  ({ inputSize }) => css`
    position: relative;
    &:hover {
      svg:first-child {
        fill: ${({ theme }) => theme.input.icon.hover};
      }
    }
    svg {
      position: absolute;
      top: ${inputSize && INPUT_SIZES[inputSize]
        ? `${INPUT_SIZES[inputSize] / 2 - 2.5}px`
        : `17.5px`};
      right: 13px;
      z-index: 1;
    }
  `
);

export const Input = styled.input<InputProps>(
  ({ theme: { input }, inputSize }) => css`
    background-color: ${input.backgroundColor.normal};
    border: 1px ${input.borderColor.normal} solid;
    border-radius: 4px;
    color: ${input.color.normal};
    height: ${inputSize && `${INPUT_SIZES[inputSize]}px`
      ? `${INPUT_SIZES[inputSize]}px`
      : '40px'};
    width: 100%;
    padding-left: 12px;
    padding-right: 30px;
    font-size: 14px;

    &::placeholder {
      color: ${input.color.placeholder.normal};
      font-size: 14px;
    }
    &:hover {
      border-color: ${input.borderColor.hover};
    }
    &:focus {
      outline: none;
      border-color: ${input.borderColor.focus};
      &::placeholder {
        color: transparent;
      }
    }
    &:read-only {
      color: ${input.color.readOnly};
      border: none;
      background-color: ${input.backgroundColor.readOnly};
      &:focus {
        &::placeholder {
          color: ${input.color.placeholder.readOnly};
        }
      }
      cursor: not-allowed;
    }
  `
);

export const OptionList = styled.ul`
  position: absolute;
  top: 100%;
  left: 0;
  max-height: 228px;
  margin-top: 4px;
  background-color: ${({ theme }) => theme.select.backgroundColor.normal};
  border: 1px ${({ theme }) => theme.select.borderColor.normal} solid;
  border-radius: 4px;
  font-size: 14px;
  line-height: 18px;
  color: ${({ theme }) => theme.select.color.normal};
  overflow-y: auto;
  z-index: 10;
  max-width: 300px;
  min-width: 100%;
  align-items: center;
  & div {
    white-space: nowrap;
  }
  &::-webkit-scrollbar {
    -webkit-appearance: none;
    width: 7px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 4px;
    background-color: ${({ theme }) =>
      theme.select.optionList.scrollbar.backgroundColor};
  }

  &::-webkit-scrollbar:horizontal {
    height: 7px;
  }
`;

export const Option = styled.li<OptionProps>`
  display: flex;
  align-items: center;
  list-style: none;
  padding: 10px 12px;
  transition: all 0.2s ease-in-out;
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  gap: 5px;
  color: ${({ theme, disabled }) =>
    theme.select.color[disabled ? 'disabled' : 'normal']};

  &:hover {
    background-color: ${({ theme, disabled }) =>
      theme.select.backgroundColor[disabled ? 'normal' : 'hover']};
  }

  &:active {
    background-color: ${({ theme }) => theme.select.backgroundColor.active};
  }
`;

export const SelectedOption = styled.li<{ isThemeMode?: boolean }>`
  display: flex;
  padding-right: ${({ isThemeMode }) => (isThemeMode ? '' : '16px')};
  list-style-position: inside;
  white-space: nowrap;
  & svg {
    path {
      fill: ${({ theme }) => theme.defaultIconColor};
    }
  }
  & div {
    display: none;
  }
`;
