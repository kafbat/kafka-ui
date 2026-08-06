import styled, { css } from 'styled-components';

export const Wrapper = styled.div`
  background-color: ${({ theme }) => theme.viewer.wrapper.backgroundColor};
  padding: 8px 16px;
  .ace_active-line {
    background-color: ${({ theme }) =>
      theme.default.backgroundColor} !important;
  }
  .ace_line {
    color: ${({ theme }) => theme.viewer.wrapper.color} !important;
  }
`;

export const ViewToggle = styled.nav`
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
`;

export const ViewToggleButton = styled.button<{ $active?: boolean }>(
  ({ theme, $active }) => css`
    background-color: ${theme.secondaryTab.backgroundColor[
      $active ? 'active' : 'normal'
    ]};
    color: ${theme.secondaryTab.color[$active ? 'active' : 'normal']};
    padding: 4px 12px;
    height: 28px;
    font-size: 12px;
    border: 1px solid ${theme.layout.stuffBorderColor};
    cursor: pointer;
    &:hover {
      background-color: ${theme.secondaryTab.backgroundColor.hover};
      color: ${theme.secondaryTab.color.hover};
    }
    &:first-child {
      border-radius: 4px 0 0 4px;
    }
    &:last-child {
      border-radius: 0 4px 4px 0;
    }
    &:not(:last-child) {
      border-right: 0;
    }
  `
);
