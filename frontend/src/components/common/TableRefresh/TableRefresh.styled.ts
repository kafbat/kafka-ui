import styled, { css } from 'styled-components';

const segment = css`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  height: ${({ theme }) => theme.button.height.M};
  font-size: ${({ theme }) => theme.button.fontSize.M};
  font-weight: 500;
  white-space: nowrap;
  background: ${({ theme }) => theme.button.secondary.backgroundColor.normal};
  color: ${({ theme }) => theme.button.secondary.color.normal};

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.button.secondary.backgroundColor.hover};
    cursor: pointer;
  }
  &:active:not(:disabled) {
    background: ${({ theme }) => theme.button.secondary.backgroundColor.active};
  }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  & svg {
    fill: ${({ theme }) => theme.button.secondary.color.normal};
  }
`;

export const Group = styled.div`
  display: inline-flex;
  align-items: stretch;
`;

export const RefreshButton = styled.button`
  ${segment};
  padding: 0 12px;
  border-radius: 4px 0 0 4px;
`;

export const IntervalButton = styled.button`
  ${segment};
  padding: 0 8px;
  border-radius: 0 4px 4px 0;
  border-left: 1px solid
    ${({ theme }) => theme.button.secondary.backgroundColor.active};
`;

export const Rate = styled.span`
  font-variant-numeric: tabular-nums;
`;
