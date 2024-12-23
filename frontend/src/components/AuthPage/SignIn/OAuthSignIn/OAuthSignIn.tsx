import React, { ElementType } from 'react';
import GitHubIcon from 'components/common/Icons/GitHubIcon';
import GoogleIcon from 'components/common/Icons/GoogleIcon';
import CognitoIcon from 'components/common/Icons/CognitoIcon';
import OktaIcon from 'components/common/Icons/OktaIcon';
import KeycloakIcon from 'components/common/Icons/KeycloakIcon';
import ServiceImage from 'components/common/Icons/ServiceImage';
import { OAuthProvider } from 'generated-sources';
import { useLocation } from 'react-router-dom';
import AlertIcon from 'components/common/Icons/AlertIcon';

import * as S from './OAuthSignIn.styled';
import AuthCard from './AuthCard/AuthCard';

interface Props {
  oAuthProviders: OAuthProvider[] | undefined;
}

const ServiceIconMap: Record<string, ElementType> = {
  github: GitHubIcon,
  google: GoogleIcon,
  cognito: CognitoIcon,
  keycloak: KeycloakIcon,
  okta: OktaIcon,
  unknownService: ServiceImage,
};

function OAuthSignIn({ oAuthProviders }: Props) {
  const { search } = useLocation();

  return (
    <S.OAuthSignInStyled>
      {search.includes('error') && (
        <S.ErrorMessage>
          <AlertIcon />
          <S.ErrorMessageText>Invalid credentials</S.ErrorMessageText>
        </S.ErrorMessage>
      )}
      {oAuthProviders?.map((provider) => (
        <AuthCard
          key={provider.clientName}
          authPath={provider.authorizationUri}
          Icon={
            ServiceIconMap[
              provider.clientName?.toLowerCase() || 'unknownService'
            ]
          }
          serviceName={provider.clientName || ''}
        />
      ))}
    </S.OAuthSignInStyled>
  );
}

export default OAuthSignIn;
