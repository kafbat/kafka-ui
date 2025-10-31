import styled from 'styled-components';

export const TagsWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  word-break: break-word;
  white-space: pre-wrap;
  span {
    color: rgb(76, 76, 255) !important;
    &:hover {
      color: rgb(23, 23, 207) !important;
    }
  }
`;
