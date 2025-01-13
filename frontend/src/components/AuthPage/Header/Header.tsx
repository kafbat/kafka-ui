import React from 'react';

import * as S from './Header.styled';
import HeaderLogo from './HeaderLogo';

function Header() {
  return (
    <S.HeaderStyled>
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />
      {Array(2).fill(<S.HeaderCell />)}
      {Array(4).fill(<S.HeaderCell $sections={2} />)}
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(3).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      {Array(2).fill(<S.HeaderCell $sections={3} />)}
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />

      {Array(3).fill(<S.HeaderCell $sections={2} />)}
      {Array(8).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(3).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(6).fill(<S.HeaderCell />)}
      {Array(3).fill(<S.HeaderCell $sections={3} />)}
      <S.HeaderCell />
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />

      <S.HeaderCell $sections={2} />
      <S.HeaderCell />
      <S.HeaderCell $sections={2} />
      <S.HeaderCell />
      <S.HeaderCell $sections={3} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      <S.HeaderCell />
      <S.HeaderCell $sections={2} />
      {Array(3).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />
      <S.HeaderCell />
      {Array(3).fill(<S.HeaderCell $sections={2} />)}
      <S.HeaderCell />
      {Array(3).fill(<S.HeaderCell $sections={3} />)}
      {Array(3).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />
      <S.HeaderCell $sections={2} />

      <S.HeaderCell />
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />
      {Array(2).fill(<S.HeaderCell />)}
      {Array(5).fill(<S.HeaderCell $sections={2} />)}
      {Array(2).fill(<S.HeaderCell />)}

      <HeaderLogo />

      {Array(5).fill(<S.HeaderCell $sections={2} />)}
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={2} />
      {Array(2).fill(<S.HeaderCell />)}
      <S.HeaderCell $sections={3} />
      <S.HeaderCell />
      <S.HeaderCell $sections={2} />
    </S.HeaderStyled>
  );
}

export default Header;
