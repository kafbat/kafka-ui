import React, { ElementType } from 'react';
import GitHubIcon from 'components/common/Icons/GitHubIcon';
import GoogleIcon from 'components/common/Icons/GoogleIcon';
import ServiceImage from 'components/common/Icons/ServiceImage';
import { OAuthProvider } from 'generated-sources';

import * as S from './OAuthSignIn.styled';
import AuthCard from './AuthCard/AuthCard';

interface Props {
  oAuthProviders: OAuthProvider[] | undefined;
}

const ServiceIconMap: Record<string, ElementType> = {
  github: GitHubIcon,
  google: GoogleIcon,
  unknownService: ServiceImage,
};

function OAuthSignIn({ oAuthProviders }: Props) {
  return (
    <S.OAuthSignInStyled>
      {oAuthProviders?.map((provider) => (
        <AuthCard
          key={provider.clientName}
          authPath={provider.authorizationUri}
          Icon={ServiceIconMap[provider.clientName?.toLowerCase() || 'unknownService']}
          serviceName={provider.clientName || ''}
        />
      ))}
    </S.OAuthSignInStyled>
  );
}

export default OAuthSignIn;
