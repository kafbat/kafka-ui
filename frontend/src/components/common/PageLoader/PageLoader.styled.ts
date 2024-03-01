import styled from 'styled-components';

export interface PageLoaderProps {
  fullSize?: boolean;
}

export const Wrapper = styled.div<PageLoaderProps>`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  background-color: ${({ theme }) => theme.default.backgroundColor};
  ${({ fullSize }) => (fullSize ? `min-height: 100vh;` : 'padding-top: 15%;')}
`;
