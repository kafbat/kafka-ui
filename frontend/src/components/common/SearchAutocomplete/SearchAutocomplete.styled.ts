import styled, { css } from 'styled-components';
import { ComponentProps } from 'react';

export interface InputProps extends ComponentProps<'input'> {
  inputSize?: 'S' | 'M' | 'L';
  searchIcon?: boolean;
}

const INPUT_SIZES = {
  S: 24,
  M: 32,
  L: 40,
};

interface OptionProps {
  disabled?: boolean;
  isHighlighted?: boolean;
}

export const Wrapper = styled.div`
  position: relative;
  min-width: 200px;
  &:hover {
    svg:first-child {
      fill: ${({ theme }) => theme.input.icon.hover};
    }
  }
  svg:first-child {
    position: absolute;
    top: 8px;
    line-height: 0;
    z-index: 1;
    left: 12px;
    right: unset;
    height: 16px;
    width: 16px;
    fill: ${({ theme }) => theme.input.icon.color};
  }
  svg:last-child {
    position: absolute;
    top: 8px;
    line-height: 0;
    z-index: 1;
    left: unset;
    right: 12px;
    height: 16px;
    width: 16px;
  }
`;

export const Input = styled.input<InputProps>(
  ({ theme: { input }, inputSize, searchIcon }) => css`
    background-color: ${input.backgroundColor.normal};
    border: 1px ${input.borderColor.normal} solid;
    border-radius: 4px;
    color: ${input.color.normal};
    height: ${inputSize ? `${INPUT_SIZES[inputSize]}px` : '32px'};
    width: 100%;
    padding-left: ${searchIcon ? '36px' : '12px'};
    padding-right: 30px;
    font-size: 14px;
    box-sizing: border-box;

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
      cursor: not-allowed;
    }
  `
);

export type StyledInputProps = ComponentProps<typeof Input>;

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
  max-width: 100%;
  min-width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
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
  background-color: ${({ isHighlighted, theme }) =>
    isHighlighted ? theme.select.backgroundColor.hover : 'transparent'};

  &:hover {
    background-color: ${({ theme, disabled }) =>
      disabled ? 'transparent' : theme.select.backgroundColor.hover};
  }

  &:active {
    background-color: ${({ theme }) => theme.select.backgroundColor.active};
  }
`;

export const IconButtonWrapper = styled.span.attrs(() => ({
  role: 'button',
  tabIndex: 0,
}))`
  display: inline-block;
  &:hover {
    cursor: pointer;
  }
`;
