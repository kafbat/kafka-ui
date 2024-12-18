import React from 'react';
import { AuthType, OAuthProvider } from 'generated-sources';

import BasicSignIn from './BasicSignIn/BasicSignIn';
import * as S from './SignIn.styled';
import OAuthSignIn from './OAuthSignIn/OAuthSignIn';

interface Props {
  authType?: AuthType;
  oAuthProviders?: OAuthProvider[];
}

function SignInForm({ authType, oAuthProviders }: Props) {
  return (
    <S.SignInStyled>
      <S.SignInTitle>Sign in</S.SignInTitle>
      {(authType === AuthType.LDAP || authType === AuthType.LOGIN_FORM) && (
        <BasicSignIn />
      )}
      {authType === AuthType.OAUTH2 && (
        <OAuthSignIn oAuthProviders={oAuthProviders} />
      )}
    </S.SignInStyled>
  );
}

export default SignInForm;
