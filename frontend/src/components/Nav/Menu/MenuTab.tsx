import React, { FC } from 'react';
import { ServerStatus } from 'generated-sources';

import * as S from './styled';

export interface MenuTabProps {
  title: string;
  status: ServerStatus;
  isOpen: boolean;
  onClick: () => void;
}

const MenuTab: FC<MenuTabProps> = ({ title, status, isOpen, onClick }) => (
  <S.MenuItem $variant="primary" onClick={onClick}>
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
