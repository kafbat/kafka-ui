import React, { type FC } from 'react';
import { ServerStatus } from 'generated-sources';

import * as S from './styled';

export interface MenuTabProps {
  title: string;
  status: ServerStatus;
  isOpen: boolean;
  toggleClusterMenu: () => void;
}

const MenuTab: FC<MenuTabProps> = ({
  title,
  toggleClusterMenu,
  status,
  isOpen,
}) => (
  <S.MenuItem $variant="primary" onClick={toggleClusterMenu}>
    <S.ContentWrapper>
      <S.StatusIconWrapper>
        <S.StatusIcon status={status} aria-label="status">
          <title>{status}</title>
        </S.StatusIcon>
      </S.StatusIconWrapper>

      <S.Title title={title}>{title}</S.Title>
    </S.ContentWrapper>

    <S.ChevronWrapper>
      <S.ChevronIcon $isOpen={isOpen} />
    </S.ChevronWrapper>
  </S.MenuItem>
);

export default MenuTab;
