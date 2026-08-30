import styled from 'styled-components';
import { SpinnerProps } from 'components/common/Spinner/types';

export const Spinner = styled.div<SpinnerProps>`
  border-width: ${(props) => props.$borderWidth}px;
  border-style: solid;
  border-color: ${({ theme }) => theme.pageLoader.borderColor};
  border-bottom-color: ${({ $emptyBorderColor, theme }) =>
    $emptyBorderColor ? 'transparent' : theme.pageLoader.borderBottomColor};
  border-radius: 50%;
  width: ${(props) => props.$size}px;
  height: ${(props) => props.$size}px;
  margin-left: ${(props) => props.$marginLeft}px;
  animation: spin 1.3s linear infinite;

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
`;
