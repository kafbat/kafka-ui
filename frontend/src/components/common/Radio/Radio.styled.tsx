import styled, { css } from 'styled-components';

import { ActiveState } from './types';

export const Item = styled.div<{
  $isActive?: boolean;
  $activeState?: ActiveState;
}>`
  background-color: ${({ theme }) => theme.radio.default.backgroundColor};
  border: 1px solid ${({ theme }) => theme.radio.default.borderColor};
  color: ${({ theme }) => theme.radio.default.color};
  padding: 0 16px;
  cursor: pointer;
  height: 32px;
  line-height: 32px;

  ${({ $isActive, $activeState, theme }) => {
    if ($isActive) {
      return css`
        color: ${$activeState?.color || theme.radio.default.activeColor};
        background-color: ${$activeState?.background ||
        theme.radio.default.activeBackgroundColor};
        border-color: ${$activeState?.background ||
        theme.radio.default.borderColor};
      `;
    }
    return css``;
  }}
`;

export const Container = styled.div`
  display: flex;

  ${Item}:first-child {
    border-radius: 4px 0px 0px 4px;
  }

  ${Item}:last-child {
    border-radius: 0px 4px 4px 0px;
  }
`;
