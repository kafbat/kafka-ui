import React, { ElementType } from 'react';
import ServiceImage from 'components/common/Icons/ServiceImage';
import { useNavigate } from 'react-router-dom';

import * as S from './AuthCard.styled';

interface Props {
  serviceName: string;
  authPath: string | undefined;
  Icon?: ElementType;
}

function AuthCard({ serviceName, authPath, Icon = ServiceImage }: Props) {
  const navigate = useNavigate();

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
        onClick={() => navigate(`${window.basePath}${authPath}`)}
      >
        Log in with {serviceName}
      </S.ServiceButton>
    </S.AuthCardStyled>
  );
}

export default AuthCard;
