import styled from 'styled-components';

export const DangerText = styled.span`
  color: ${({ theme }) => theme.circularAlert.color.error};
`;

export const BootstrapServersValue = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 6px;
`;

export const CopyButton = styled.button`
  appearance: none;
  background: none;
  border: none;
  padding: 2px;
  cursor: pointer;
  color: inherit;
  display: inline-flex;
  align-items: center;
  opacity: 0.6;

  &:hover {
    opacity: 1;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.3;
  }
`;
