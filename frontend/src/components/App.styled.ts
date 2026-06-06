import styled from 'styled-components';

export const Layout = styled.div`
  min-height: 100vh;
  background: ${({ theme }) => theme.surface.canvas};

  @media screen and (max-width: 1023px) {
    min-width: initial;
  }
`;
