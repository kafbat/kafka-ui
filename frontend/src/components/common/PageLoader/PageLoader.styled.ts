import styled from 'styled-components';

export interface PageLoaderProps {
  fullSize?: boolean;
  offsetY?: number;
}

export const Wrapper = styled.div<PageLoaderProps>`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  background-color: ${({ theme }) => theme.default.backgroundColor};

  height: ${({ offsetY }) =>
    offsetY !== undefined ? `calc(100vh - ${offsetY}px)` : '100%'};

  ${({ fullSize, offsetY }) =>
    !fullSize && offsetY === undefined ? 'padding-top: 15%;' : ''}

  ${({ fullSize }) => (fullSize ? 'min-height: 100vh;' : '')}
`;
