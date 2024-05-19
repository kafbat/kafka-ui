import styled from 'styled-components';

export const List = styled.ul.attrs({ role: 'menu' })`
  padding: 2px 4px 6px 4px;

  & > & {
    padding: 0 0 0 8px;
  }

  & * {
    margin-bottom: 2px;
  }
`;
