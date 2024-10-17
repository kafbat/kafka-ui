import React from 'react';
import GitHubIcon from 'components/common/Icons/GitHubIcon';
import GoogleIcon from 'components/common/Icons/GoogleIcon';

import * as S from './ServiceSignIn.styled';
import AuthCard from './AuthCard/AuthCard';

function ServiceSignIn() {
  return (
    <S.ServiceSignInStyled>
      <AuthCard Icon={GitHubIcon} serviceName="Github" />
      <AuthCard Icon={GoogleIcon} serviceName="Google" />
      <AuthCard serviceName="Service" />
    </S.ServiceSignInStyled>
  );
}

export default ServiceSignIn;
