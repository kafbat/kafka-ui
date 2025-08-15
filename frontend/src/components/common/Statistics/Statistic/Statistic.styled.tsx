import styled from 'styled-components';

export const Container = styled.div`
  width: 216px;
  height: 68px;
  border-radius: 12px;
  padding: 12px 16px;
  background: ${({ theme }) => theme.kafkaConectClusters.statistic.background};
`;

export const Header = styled.span`
  font-weight: 500;
  font-size: 12px;
  line-height: 16px;
  color: ${({ theme }) => theme.kafkaConectClusters.statistic.header.color};
`;
export const Footer = styled.div`
  display: flex;
  justify-content: space-between;
`;

export const Count = styled.div`
  font-weight: 500;
  font-size: 16px;
  line-height: 24px;
  color: ${({ theme }) => theme.kafkaConectClusters.statistic.count.color};
`;
