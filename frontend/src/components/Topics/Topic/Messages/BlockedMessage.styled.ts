import styled from 'styled-components';

export const Row = styled.tr`
  background: ${({ theme }) => theme.alert.color.warning};

  td {
    white-space: normal;
  }
`;

export const Summary = styled.div`
  min-width: 280px;
`;

export const Title = styled.div`
  color: ${({ theme }) => theme.alert.textColor.warning};
  font-weight: 600;
  line-height: 20px;
`;

export const Description = styled.div`
  color: ${({ theme }) => theme.alert.textColor.warning};
  font-size: 12px;
  line-height: 18px;
  margin-top: 2px;
`;

export const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 8px;
`;
