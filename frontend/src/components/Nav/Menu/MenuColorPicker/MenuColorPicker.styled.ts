import styled from 'styled-components';
import { ClusterColorKey } from 'theme/theme';

export const Container = styled.div`
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(2, auto);
  padding: 0 8px;
  gap: 8px;
  border-radius: 8px;
  cursor: auto;
`;

export const ColorCircle = styled.div<{ $colorKey: ClusterColorKey }>`
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin: 4px;
  background-color: ${({ $colorKey, theme }) =>
    theme.clusterColorPicker.backgroundColor[$colorKey]};
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    outline: ${({ theme }) => `1px solid ${theme.clusterColorPicker.outline}`};
  }

  &:active {
    outline: ${({ theme }) => `2px solid ${theme.clusterColorPicker.outline}`};
  }

  ${({ $colorKey, theme }) =>
    $colorKey === 'transparent' &&
    `
        border: 1px solid ${theme.clusterColorPicker.transparentCircle.border};
        &:before {
            content: '\\2716';
            font-size: 8px;
            color: ${theme.clusterColorPicker.transparentCircle.cross};
        }
        &:hover {
            border: none;
        }
        &:active {
            border: none;
        }
    `}
`;
