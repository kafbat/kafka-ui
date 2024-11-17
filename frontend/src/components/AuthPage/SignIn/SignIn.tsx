import React from 'react';
import { AppAuthenticationSettings, AuthType } from 'generated-sources';

import BasicSignIn from './BasicSignIn/BasicSignIn';
import * as S from './SignIn.styled';
import OAuthSignIn from './OAuthSignIn/OAuthSignIn';

interface Props {
  appAuthenticationSettings: AppAuthenticationSettings;
}

function SignInForm({ appAuthenticationSettings }: Props) {
  const { authType, oAuthProviders } = appAuthenticationSettings;

  return (
    <S.SignInStyled>
      <S.SignInTitle>Sign in</S.SignInTitle>
      {(authType === AuthType.LDAP ||
        authType === AuthType.LOGIN_FORM) && <BasicSignIn />}
      {authType === AuthType.OAUTH2 && (
        <OAuthSignIn oAuthProviders={oAuthProviders} />
      )}
    </S.SignInStyled>
  );
}

export default SignInForm;
