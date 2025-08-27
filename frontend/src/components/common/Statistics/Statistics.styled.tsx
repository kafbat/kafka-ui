import styled from 'styled-components';

export const Container = styled.div`
  padding: 16px;
  display: flex;
  gap: 4px;
  background: ${({ theme }) => theme.kafkaConectClusters.statistics.background};
`;
