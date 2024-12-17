import React from 'react';
import { useAuthSettings } from 'lib/hooks/api/appConfig';

import Header from './Header/Header';
import SignIn from './SignIn/SignIn';
import * as S from './AuthPage.styled';

function AuthPage() {
  const { data } = useAuthSettings();

  return (
    <S.AuthPageStyled>
      <Header />
      {data && (
        <SignIn authType={data.authType} oAuthProviders={data.oAuthProviders} />
      )}
    </S.AuthPageStyled>
  );
}

export default AuthPage;
