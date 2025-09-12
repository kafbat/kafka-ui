import React, { Dispatch, type FC, SetStateAction } from 'react';
import { ServerStatus } from 'generated-sources';
import MenuColorPicker from 'components/Nav/Menu/MenuColorPicker/MenuColorPicker';
import { ClusterColorKey } from 'theme/theme';

import * as S from './styled';

export interface MenuTabProps {
  title: string;
  status: ServerStatus;
  isOpen: boolean;
  toggleClusterMenu: () => void;
  onClusterNameClick: () => void;
  setColorKey: Dispatch<SetStateAction<ClusterColorKey>>;
  isActive?: boolean;
}

const MenuTab: FC<MenuTabProps> = ({
  title,
  toggleClusterMenu,
  onClusterNameClick,
  status,
  isOpen,
  setColorKey,
  isActive = false,
}) => {
  const handleNameClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onClusterNameClick();
  };

  const handleChevronClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    toggleClusterMenu();
  };

  return (
    <S.MenuItem $variant="primary" $isActive={isActive}>
      <S.ContentWrapper onClick={handleNameClick} style={{ cursor: 'pointer' }}>
        <S.StatusIconWrapper>
          <S.StatusIcon status={status} aria-label="status">
            <title>{status}</title>
          </S.StatusIcon>
        </S.StatusIconWrapper>

        <S.Title title={title}>{title}</S.Title>
      </S.ContentWrapper>

      <S.ActionsWrapper>
        <S.ColorPickerWrapper>
          <MenuColorPicker setColorKey={setColorKey} />
        </S.ColorPickerWrapper>

        <S.ChevronClickArea onClick={handleChevronClick}>
          <S.ChevronWrapper>
            <S.ChevronIcon $isOpen={isOpen} />
          </S.ChevronWrapper>
        </S.ChevronClickArea>
      </S.ActionsWrapper>
    </S.MenuItem>
  );
};

export default MenuTab;
