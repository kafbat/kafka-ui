import styled, { css } from 'styled-components';

import { RadioItemType } from './types';

export const Item = styled.div<{
  $isActive?: boolean;
  $itemType: RadioItemType;
}>`
  ${({ theme, $itemType }) => css`
    background-color: ${theme.acl.create.radioButtons[$itemType].normal
      .background};
    border: 1px solid ${theme.acl.create.radioButtons[$itemType].normal.border};
    color: ${theme.acl.create.radioButtons[$itemType].normal.text};
    padding: 0 16px;
    cursor: pointer;
    height: 32px;
    line-height: 32px;
  `}

  ${({ $isActive, $itemType, theme }) => {
    if ($isActive) {
      return css`
        color: ${theme.acl.create.radioButtons[$itemType].active.text};
        background-color: ${theme.acl.create.radioButtons[$itemType].active.background};
        border-color: ${theme.acl.create.radioButtons[$itemType].active.background}};
      `;
    }
    return css`
      &:hover {
        background: ${theme.acl.create.radioButtons[$itemType].hover
          .background};
        border: 1px solid
          ${theme.acl.create.radioButtons[$itemType].hover.border};
        color: ${theme.acl.create.radioButtons[$itemType].hover.text};
      }
    `;
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
