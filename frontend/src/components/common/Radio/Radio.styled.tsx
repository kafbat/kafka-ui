import styled, { css } from 'styled-components';

import { ActiveColor } from './types';

export const Item = styled.div<{
  $isActive?: boolean;
  $activeBg?: ActiveColor;
}>`
  background-color: white;
  border: 1px solid #e3e6e8;
  color: #73848c;
  padding: 0 16px;
  cursor: pointer;
  height: 32px;
  line-height: 32px;

  ${({ $isActive, $activeBg }) => {
    if ($isActive) {
      return css`
        color: ${$activeBg?.color || 'black'};
        background-color: ${$activeBg?.background || '#E3E6E8'};
        border-color: ${$activeBg?.background || '#E3E6E8'};
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
