import React from 'react';

import BasicSignIn from './BasicSignIn/BasicSignIn';
import * as S from './SignIn.styled';
import ServiceSignIn from './ServiceSignIn/ServiceSignIn';

interface Props {
  type: 'service' | 'basic';
}

function SignInForm({ type }: Props) {
  return (
    <S.SignInStyled>
      <S.SignInTitle>Sign in</S.SignInTitle>
      {type === 'basic' && <BasicSignIn />}
      {type === 'service' && <ServiceSignIn />}
    </S.SignInStyled>
  );
}

export default SignInForm;
