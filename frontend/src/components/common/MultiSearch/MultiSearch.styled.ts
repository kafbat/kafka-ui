import styled, { css } from 'styled-components';
import { ComponentProps } from 'react';

export interface InputProps extends ComponentProps<'input'> {
  values?: string[];
  inputSize?: 'S' | 'M' | 'L';
  searchIcon?: boolean;
  isFocused?: boolean;
}

const INPUT_SIZES = {
  S: 18,
  M: 32,
  L: 40,
};

export const Wrapper = styled.div<InputProps>(
  ({ theme: { input } }) => css`
    position: relative;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    border: 1px solid ${input.borderColor.normal};
    padding: 6px 32px 6px 12px;
    border-radius: 4px;
    min-width: 250px;
    background-color: ${input.backgroundColor.normal};

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
  `
);

export const ValuesContainer = styled.div<InputProps>(
  ({ isFocused }) => css`
    display: flex;
    align-items: center;
    flex-wrap: ${isFocused ? 'nowrap' : 'wrap'};
    gap: 8px;
    flex-grow: 1;
  `
);

export const Tag = styled.div`
  display: flex;
  align-items: center;
  background-color: ${({ theme }) => theme.tag.backgroundColor.blue};
  color: ${({ theme }) => theme.tag.color};
  border-radius: 12px;
  padding: 2px 0 2px 8px;
  font-size: 12px;
`;

export const RemoveButton = styled.button`
  background: none;
  border: none;
  margin-left: 2px;
  display: flex;
  align-items: center;
  cursor: pointer;
  color: ${({ theme }) => theme.tag.color};

  &:hover {
    color: ${({ theme }) => theme.tag.backgroundColor};
  }
`;

export const Input = styled.input<InputProps>(
  ({ theme: { input }, inputSize, values }) => css`
    flex-grow: 1;
    background-color: ${input.backgroundColor.normal};
    border: none;
    color: ${input.color.normal};
    height: ${inputSize ? `${INPUT_SIZES[inputSize]}px` : '32px'};
    font-size: 14px;
    box-sizing: border-box;
    width: ${values ? `15px` : '32px'};

    &::placeholder {
      color: ${input.color.placeholder.normal};
      font-size: 14px;
    }

    &:focus {
      outline: none;
      &::placeholder {
        color: transparent;
      }
    }
  `
);

export const IconButtonWrapper = styled.span.attrs(() => ({
  role: 'button',
  tabIndex: 0,
}))`
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover {
    cursor: pointer;
  }
`;

export const RemainingTagCount = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.input.color.normal};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;
