import styled from 'styled-components';

interface Props {
  selectSize: 'M' | 'L';
  isLive?: boolean;
  minWidth?: string;
  disabled?: boolean;
  isThemeMode?: boolean;
}

interface OptionProps {
  disabled?: boolean;
}

export const Select = styled.ul<Props>`
  position: relative;
  list-style: none;
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: space-between;
  height: ${(props) => (props.selectSize === 'M' ? '32px' : '40px')};
  border: 1px
    ${({ theme, disabled, isThemeMode }) => {
      if (isThemeMode) {
        return 'none';
      }
      if (disabled) {
        return theme.select.borderColor.disabled;
      }

      return theme.select.borderColor.normal;
    }}
    solid;
  border-radius: 6px;
  font-size: 14px;
  width: fit-content;
  padding-left: 16px;
  padding-right: 12px;
  background: ${({ theme, isThemeMode }) =>
    isThemeMode ? 'transparent' : theme.select.backgroundColor.normal};
  color: ${({ theme, disabled }) =>
    disabled ? theme.select.color.disabled : theme.select.color.normal};
  min-width: ${({ minWidth }) => minWidth || 'auto'};
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  transition:
    background-color 120ms ease,
    border-color 120ms ease,
    box-shadow 120ms ease,
    color 120ms ease;

  &:hover {
    background: ${({ theme, isThemeMode }) =>
      isThemeMode ? theme.surface.muted : theme.select.backgroundColor.hover};
    color: ${({ theme, disabled }) =>
      disabled ? theme.select.color.disabled : theme.select.color.hover};
    border-color: ${({ theme, disabled }) =>
      disabled
        ? theme.select.borderColor.disabled
        : theme.select.borderColor.hover};
  }
  &:focus {
    outline: none;
    color: ${({ theme }) => theme.select.color.active};
    border-color: ${({ theme }) => theme.select.borderColor.active};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.surface.ring};
  }
  &:disabled {
    color: ${({ theme }) => theme.select.color.disabled};
    border-color: ${({ theme }) => theme.select.borderColor.disabled};
  }
`;

export const SelectedOptionWrapper = styled.div`
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const OptionList = styled.ul`
  position: absolute;
  top: 100%;
  left: 0;
  max-height: 228px;
  margin-top: 4px;
  background-color: ${({ theme }) => theme.select.backgroundColor.normal};
  border: 1px ${({ theme }) => theme.select.optionList.borderColor} solid;
  border-radius: 8px;
  box-shadow: ${({ theme }) => theme.surface.shadowLg};
  font-size: 14px;
  line-height: 18px;
  color: ${({ theme }) => theme.select.color.normal};
  overflow-y: auto;
  z-index: 12;
  max-width: 300px;
  min-width: 100%;
  align-items: center;
  & div {
    white-space: nowrap;
  }
  &::-webkit-scrollbar {
    -webkit-appearance: none;
    width: 7px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 4px;
    background-color: ${({ theme }) =>
      theme.select.optionList.scrollbar.backgroundColor};
  }

  &::-webkit-scrollbar:horizontal {
    height: 7px;
  }
`;

export const Option = styled.li<OptionProps>`
  display: flex;
  align-items: center;
  list-style: none;
  padding: 10px 12px;
  transition:
    background-color 120ms ease,
    color 120ms ease;
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  gap: 5px;
  color: ${({ theme, disabled }) =>
    theme.select.color[disabled ? 'disabled' : 'normal']};

  &:hover {
    color: ${({ theme, disabled }) =>
      theme.select.color[disabled ? 'disabled' : 'hover']};
    background-color: ${({ theme, disabled }) =>
      theme.select.backgroundColor[disabled ? 'normal' : 'hover']};
  }

  &:active {
    color: ${({ theme, disabled }) =>
      theme.select.color[disabled ? 'disabled' : 'active']};
    background-color: ${({ theme }) => theme.select.backgroundColor.active};
  }
`;

export const SelectedOption = styled.li<{ isThemeMode?: boolean }>`
  display: flex;
  padding-right: ${({ isThemeMode }) => (isThemeMode ? '' : '16px')};
  list-style-position: inside;
  white-space: nowrap;
  align-items: center;
  & svg {
    path {
      fill: ${({ theme }) => theme.defaultIconColor};
    }
  }
  & div {
    display: none;
  }
`;
