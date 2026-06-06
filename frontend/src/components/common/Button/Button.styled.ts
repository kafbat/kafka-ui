import styled from 'styled-components';

export interface ButtonProps {
  buttonType: 'primary' | 'secondary' | 'danger' | 'text';
  buttonSize: 'S' | 'M' | 'L';
}

const StyledButton = styled.button<ButtonProps>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: ${({ buttonSize }) => (buttonSize === 'S' ? '0 10px' : '0 14px')};
  border: 1px solid
    ${({ buttonType, theme }) =>
      buttonType === 'secondary' ? theme.surface.borderStrong : 'transparent'};
  border-radius: 6px;
  white-space: nowrap;
  letter-spacing: 0;
  transition:
    background-color 120ms ease,
    border-color 120ms ease,
    box-shadow 120ms ease,
    color 120ms ease,
    transform 120ms ease;

  background: ${({ buttonType, theme }) =>
    theme.button[buttonType].backgroundColor.normal};

  color: ${({ buttonType, theme }) => theme.button[buttonType].color.normal};
  height: ${({ theme, buttonSize }) => theme.button.height[buttonSize]};
  font-size: ${({ theme, buttonSize }) => theme.button.fontSize[buttonSize]};
  font-weight: 500;
  box-shadow: ${({ buttonType, theme }) =>
    buttonType === 'primary' || buttonType === 'danger'
      ? theme.surface.shadow
      : 'none'};

  &:hover {
    background: ${({ buttonType, theme }) =>
      theme.button[buttonType].backgroundColor.hover};
    color: ${({ buttonType, theme }) => theme.button[buttonType].color.hover};
    border-color: ${({ buttonType, theme }) =>
      buttonType === 'secondary' ? theme.surface.borderStrong : 'transparent'};
    cursor: pointer;
  }

  &:active {
    background: ${({ buttonType, theme }) =>
      theme.button[buttonType].backgroundColor.active};
    color: ${({ buttonType, theme }) => theme.button[buttonType].color.active};
    transform: translateY(1px);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px ${({ theme }) => theme.surface.ring};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: ${({ buttonType, theme }) =>
      theme.button[buttonType].backgroundColor.disabled};
    color: ${({ buttonType, theme }) =>
      theme.button[buttonType].color.disabled};
  }

  & a {
    color: ${({ theme }) => theme.button.primary.color.normal};
  }

  & svg {
    flex: 0 0 auto;
    fill: ${({ theme, disabled, buttonType }) =>
      disabled
        ? theme.button[buttonType].color.disabled
        : theme.button[buttonType].color.normal};
  }
`;

export default StyledButton;
