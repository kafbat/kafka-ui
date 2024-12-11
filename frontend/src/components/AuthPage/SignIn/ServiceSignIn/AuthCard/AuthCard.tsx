import React, { ElementType } from 'react';
import { Button } from 'components/common/Button/Button';
import ServiceImage from 'components/common/Icons/ServiceImage';

import * as S from './AuthCard.styled';

interface Props {
  serviceName: string;
  Icon?: ElementType;
}

function AuthCard({ serviceName, Icon = ServiceImage }: Props) {
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
      <Button
        buttonSize="L"
        buttonType="primary"
        onClick={() => console.log('click')}
        style={{ borderRadius: '8px', fontSize: '14px' }}
      >
        Log in with {serviceName}
      </Button>
    </S.AuthCardStyled>
  );
}

export default AuthCard;
