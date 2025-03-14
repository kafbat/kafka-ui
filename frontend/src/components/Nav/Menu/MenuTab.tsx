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
  setColorKey: Dispatch<SetStateAction<ClusterColorKey>>;
}

const MenuTab: FC<MenuTabProps> = ({
  title,
  toggleClusterMenu,
  status,
  isOpen,
  setColorKey,
}) => (
  <S.MenuItem $variant="secondary" onClick={toggleClusterMenu}>
    <S.ContentWrapper>
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

      <S.ChevronWrapper>
        <S.ChevronIcon $isOpen={isOpen} />
      </S.ChevronWrapper>
    </S.ActionsWrapper>
  </S.MenuItem>
);

export default MenuTab;
