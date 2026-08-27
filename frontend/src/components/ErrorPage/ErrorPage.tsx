import React from 'react';
import { Button } from 'components/common/Button/Button';

import * as S from './ErrorPage.styled';
import { getErrorInfoByCode } from './utils';

interface Props {
  status?: number;
  offsetY?: number;
  text?: string;
  resourceName?: string;
  btnText?: string;
  onClick?: () => void;
}

const ErrorPage: React.FC<Props> = ({
  status,
  text,
  btnText = 'Refresh',
  onClick,
  offsetY = 154,
  resourceName,
}) => {
  const {
    title,
    icon,
    text: errorText,
  } = getErrorInfoByCode(status, resourceName);
  return (
    <S.Wrapper $offsetY={offsetY}>
      {icon}

      <S.TextContainer>
        <S.Title>{title}</S.Title>

        <S.Text>{text || errorText}</S.Text>
      </S.TextContainer>

      <Button buttonType="secondary" buttonSize="M" onClick={onClick}>
        {btnText}
      </Button>
    </S.Wrapper>
  );
};

export default ErrorPage;
