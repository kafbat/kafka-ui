import styled, { css } from 'styled-components';
import { ServerStatus } from 'generated-sources';

export const ColorPickerWrapper = styled.div`
  display: flex;
  visibility: hidden;
`;

export const MenuItem = styled('li').attrs({ role: 'menuitem' })<{
  $variant: 'primary' | 'secondary';
  $isActive?: boolean;
}>(
  ({ theme, $variant, $isActive }) => css`
    font-size: 14px;
    font-weight: ${theme.menu[$isActive ? 'primary' : $variant].fontWeight};
    min-height: 28px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    line-height: 17px;
    user-select: none;
    width: 100%;
    padding: 4px 8px;
    cursor: pointer;
    text-decoration: none;
    border-radius: 8px;
    background-color: ${$isActive
      ? theme.menu[$variant].backgroundColor.active
      : theme.menu[$variant].backgroundColor.normal};
    color: ${$isActive
      ? theme.menu[$variant].color.active
      : theme.menu[$variant].color.normal};

    &:hover {
      background-color: ${theme.menu[$variant].backgroundColor.hover};
      color: ${theme.menu[$variant].color.hover};

      ${ColorPickerWrapper} {
        visibility: visible;
      }
    }

    &:active {
      background-color: ${theme.menu[$variant].backgroundColor.active};
      color: ${theme.menu[$variant].color.active};
    }
  `
);

export const ContentWrapper = styled.div`
  display: flex;
  align-items: center;
  column-gap: 4px;
  width: 100%;
`;

export const Title = styled.div`
  width: 100%;
`;

export const StatusIconWrapper = styled.svg.attrs({
  viewBox: '0 0 6 6',
  xmlns: 'http://www.w3.org/2000/svg',
})`
  fill: none;
  width: 6px;
  height: 6px;
`;

export const StatusIcon = styled.circle.attrs({
  cx: 3,
  cy: 3,
  r: 3,
  role: 'status-circle',
})<{ status: ServerStatus }>(({ theme, status }) => {
  const statusColor: {
    [k in ServerStatus]: string;
  } = {
    [ServerStatus.ONLINE]: theme.menu.primary.statusIconColor.online,
    [ServerStatus.OFFLINE]: theme.menu.primary.statusIconColor.offline,
    [ServerStatus.INITIALIZING]:
      theme.menu.primary.statusIconColor.initializing,
  };

  return css`
    fill: ${statusColor[status]};
  `;
});

export const ChevronClickArea = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 4px 6px;
  margin: -4px -6px;
  border-radius: 4px;
`;

export const ChevronWrapper = styled.svg.attrs({
  viewBox: '0 0 10 6',
  xmlns: 'http://www.w3.org/2000/svg',
})`
  width: 10px;
  height: 6px;
  fill: none;
`;

type ChevronIconProps = { $isOpen: boolean };
export const ChevronIcon = styled.path.attrs<ChevronIconProps>(
  ({ $isOpen }) => ({
    d: $isOpen ? 'M8.99988 5L4.99988 1L0.999878 5' : 'M1 1L5 5L9 1',
  })
)<ChevronIconProps>`
  stroke: ${({ theme }) => theme.menu.primary.chevronIconColor};
`;

export const ActionsWrapper = styled.div`
  display: flex;
  align-items: center;
`;
