import styled from 'styled-components';
import { ClusterColorKey } from 'theme/theme';

export const List = styled.ul.attrs({ role: 'menu' })`
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding: 0;

  & > & {
    padding: 4px 0 2px 14px;
  }

  & * {
    margin-bottom: 0;
  }
`;

export const ClusterList = styled.ul.attrs<{ $colorKey: ClusterColorKey }>({
  role: 'menu',
})`
  border: 1px solid
    ${({ theme, $colorKey }) =>
      $colorKey === 'transparent'
        ? theme.surface.border
        : theme.clusterMenu.backgroundColor[$colorKey]};
  border-radius: 8px;
  padding: 5px;
  margin: 8px 0 0;
  background-color: ${({ theme, $colorKey }) =>
    $colorKey === 'transparent'
      ? theme.surface.panel
      : theme.clusterMenu.backgroundColor[$colorKey]};
  box-shadow: ${({ theme }) => theme.surface.shadow};
`;
