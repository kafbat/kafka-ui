import styled from 'styled-components';

export const Actions = styled.div`
  position: absolute;
  display: flex;
  align-items: center;
  top: 8px;
  right: 8px;
  gap: 8px;

  svg:first-child {
    position: initial;
    height: 16px;
    width: 16px;
    fill: ${({ theme }) => theme.input.icon.color};
  }
  svg:last-child {
    position: initial;
    height: 16px;
    width: 16px;
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
