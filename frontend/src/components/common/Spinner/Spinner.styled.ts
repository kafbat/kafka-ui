import styled, { keyframes } from 'styled-components';
import { SpinnerProps } from 'components/common/Spinner/types';

const stretch = keyframes`
  0% {
    width: 1em;
  }
  25% {
    width: 4em;
  }
  50% {
    width: 1em;
  }
  100% {
    width: 1em;
  }
`;

export const Spinner = styled.div<SpinnerProps>`
  display: flex;
  align-items: center;
  gap: ${(props) => (props.size || 80) / 7}px;
  margin-left: ${(props) => props.marginLeft}px;
  font-size: ${(props) => (props.size || 80) / 5}px;

  span {
    display: inline-block;
    width: 1em;
    height: 1em;
    border-radius: 1em;
    background-color: ${({ theme }) => theme.pageLoader.borderBottomColor};
    animation-name: ${stretch};
    animation-duration: 1.6s;
    animation-timing-function: ease;
    animation-iteration-count: infinite;
  }

  span:nth-child(1) { animation-delay: 0s; }
  span:nth-child(2) { animation-delay: 0.4s; }
  span:nth-child(3) { animation-delay: 0.8s; }
  span:nth-child(4) { animation-delay: 1.2s; }
`;