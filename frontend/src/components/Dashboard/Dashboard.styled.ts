import styled from 'styled-components';

export const Toolbar = styled.div`
  padding: 0 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: ${({ theme }) => theme.default.color.normal};
`;

export const FilterToggle = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid ${({ theme }) => theme.surface.border};
  border-radius: 8px;
  background: ${({ theme }) => theme.surface.panel};
  box-shadow: ${({ theme }) => theme.surface.shadow};
  color: ${({ theme }) => theme.surface.foregroundMuted};
  font-weight: 500;

  label {
    cursor: pointer;
  }
`;

export const ClusterNameCell = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  max-width: 100%;
  white-space: normal;
  word-break: break-word;
  font-weight: 600;
  color: ${({ theme }) => theme.surface.foreground};
`;
