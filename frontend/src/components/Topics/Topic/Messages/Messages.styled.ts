import styled from 'styled-components';

export const TableViewport = styled.div`
  max-width: 100%;
  overflow-x: auto;
`;

export const StopLoading = styled.div`
  color: ${({ theme }) => theme.pageLoader.borderColor};
  cursor: pointer;
`;
