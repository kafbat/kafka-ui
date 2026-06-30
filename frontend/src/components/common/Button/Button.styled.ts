import styled from 'styled-components';

export interface ButtonProps {
  $buttonType: 'primary' | 'secondary' | 'danger' | 'text';
  $buttonSize: 'S' | 'M' | 'L';
}

const StyledButton = styled.button<ButtonProps>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: ${({ $buttonSize }) => ($buttonSize === 'S' ? '0 8px' : '0 12px')};
  border: none;
  border-radius: 4px;
  white-space: nowrap;

  background: ${({ $buttonType, theme }) =>
    theme.button[$buttonType].backgroundColor.normal};

  color: ${({ $buttonType, theme }) => theme.button[$buttonType].color.normal};
  height: ${({ theme, $buttonSize }) => theme.button.height[$buttonSize]};
  font-size: ${({ theme, $buttonSize }) => theme.button.fontSize[$buttonSize]};
  font-weight: 500;

  &:hover {
    background: ${({ $buttonType, theme }) =>
      theme.button[$buttonType].backgroundColor.hover};
    color: ${({ $buttonType, theme }) => theme.button[$buttonType].color.hover};
    cursor: pointer;
  }

  &:active {
    background: ${({ $buttonType, theme }) =>
      theme.button[$buttonType].backgroundColor.active};
    color: ${({ $buttonType, theme }) =>
      theme.button[$buttonType].color.active};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: ${({ $buttonType, theme }) =>
      theme.button[$buttonType].backgroundColor.disabled};
    color: ${({ $buttonType, theme }) =>
      theme.button[$buttonType].color.disabled};
  }

  & a {
    color: ${({ theme }) => theme.button.primary.color.normal};
  }

  & svg {
    margin-right: 4px;
    fill: ${({ theme, disabled, $buttonType }) =>
      disabled
        ? theme.button[$buttonType].color.disabled
        : theme.button[$buttonType].color.normal};
  }
`;

export default StyledButton;
