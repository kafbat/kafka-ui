import React, { ElementType } from 'react';
import ServiceImage from 'components/common/Icons/ServiceImage';

import * as S from './AuthCard.styled';

interface Props {
  serviceName: string;
  authPath: string | undefined;
  Icon?: ElementType;
}

function AuthCard({ serviceName, authPath, Icon = ServiceImage }: Props) {
  return (
    <S.AuthCardStyled>
      <S.ServiceData>
        <Icon />
        <S.ServiceDataTextContainer>
          <S.ServiceNameStyled>{serviceName}</S.ServiceNameStyled>
          <S.ServiceTextStyled>
            Use an account issued by the organization
          </S.ServiceTextStyled>
        </S.ServiceDataTextContainer>
      </S.ServiceData>
      <S.ServiceButton
        buttonSize="L"
        buttonType="primary"
        to={`http://localhost:8080${authPath}`}
      >
        Log in with {serviceName}
      </S.ServiceButton>
    </S.AuthCardStyled>
  );
}

export default AuthCard;
