import styled from 'styled-components';

export const Navbar = styled.nav`
  display: flex;
  border-bottom: 1px ${({ theme }) => theme.primaryTab.borderColor.nav} solid;
  height: ${({ theme }) => theme.primaryTab.height};
`;

export const Tab = styled.a<{ isActive?: boolean }>`
  height: 40px;
  min-width: 96px;
  padding: 0 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  color: ${({ theme, isActive }) =>
  isActive ? theme.primaryTab.color.active : theme.primaryTab.color.normal};
  border-bottom: 1px
    ${({ theme, isActive }) =>
  isActive ? theme.primaryTab.borderColor.active : theme.default.transparentColor}
    solid;
  cursor: ${({ isActive }) => (isActive ? 'default' : 'pointer')};

  &:hover {
    color: ${({ theme, isActive }) =>
  isActive ? theme.primaryTab.color.active : theme.primaryTab.color.hover};
    border-bottom: 1px
      ${({ theme, isActive }) =>
  isActive ? theme.primaryTab.borderColor.active : theme.default.transparentColor}
      solid;
  }
`;
