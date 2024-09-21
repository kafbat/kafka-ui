import styled from 'styled-components';

export const ValueWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  font-weight: 400;

  button {
    margin: 0 10px;
  }
`;

export const Value = styled.span<{ $isDynamic?: boolean }>`
  line-height: 24px;
  margin-right: 10px;
  text-overflow: ellipsis;
  max-width: 400px;
  overflow: hidden;
  white-space: nowrap;
  font-weight: ${({ $isDynamic }) => ($isDynamic ? 600 : 400)};
`;

export const ButtonsWrapper = styled.div`
  display: flex;
`;
