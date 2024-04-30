import React, { type FC } from 'react';
import { NavLink } from 'react-router-dom';

import * as S from './styled';

export interface MenuItemProps {
  to: string;
  title: string;
  variant?: 'primary' | 'secondary';
  isActive?: boolean;
}

const MenuItem: FC<MenuItemProps> = ({
  title,
  to,
  isActive,
  variant = 'secondary',
}) => (
  <NavLink to={to} title={title}>
    <S.MenuItem $isActive={isActive} $variant={variant}>
      {title}
    </S.MenuItem>
  </NavLink>
);

export default MenuItem;
