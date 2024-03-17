import styled from 'styled-components';

export const ValueWrapper = styled.div<{ $isDynamic?: boolean }>`
  display: flex;
  justify-content: space-between;
  font-weight: ${({ $isDynamic }) => ($isDynamic ? 600 : 400)};

  button {
    margin: 0 10px;
  }
`;

export const Value = styled.span`
  line-height: 24px;
  margin-right: 10px;
  text-overflow: ellipsis;
  max-width: 400px;
  overflow: hidden;
  white-space: nowrap;
`;

export const ButtonsWrapper = styled.div`
  display: flex;
`;
