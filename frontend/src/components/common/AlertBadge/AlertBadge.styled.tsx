import styled from 'styled-components';

export const Container = styled.div`
  height: 24px;
  border-radius: 24px;
  background-color: ${({ theme }) => theme.alertBadge.background};
  display: inline-flex;
  padding: 2px 8px;
  gap: 4px;
  align-items: center;
`;

export const Content = styled.div`
  font-weight: 400;
  size: 14px;
  line-height: 20px;
  color: ${({ theme }) => theme.alertBadge.content.color};
`;
