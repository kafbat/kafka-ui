import styled from 'styled-components';

interface Props {
  checked?: boolean;
}

export const StyledLabel = styled.label`
  position: relative;
  display: inline-block;
  width: 40px;
  height: 22px;
  margin-right: 8px;
`;
export const CheckedIcon = styled.span`
  position: absolute;
  top: 1px;
  left: 24px;
  z-index: 10;
  cursor: pointer;
`;
export const UnCheckedIcon = styled.span`
  position: absolute;
  top: 2px;
  right: 23px;
  z-index: 10;
  cursor: pointer;
`;
export const StyledSlider = styled.span<Props>`
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: ${({ checked, theme }) =>
    checked ? theme.switch.checked : theme.switch.unchecked};
  transition: 0.4s;
  border-radius: 20px;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);

  &:hover {
    background-color: ${({ theme }) => theme.switch.hover};
  }

  &::before {
    position: absolute;
    content: '';
    height: 16px;
    width: 16px;
    left: ${({ checked }) => (checked ? '21px' : '3px')};
    bottom: 3px;
    background-color: ${({ theme }) => theme.switch.circle};
    transition: 0.4s;
    border-radius: 50%;
    z-index: 11;
    box-shadow: ${({ theme }) => theme.surface.shadow};
  }
`;

export const StyledInput = styled.input`
  opacity: 0;
  width: 0;
  height: 0;
`;
