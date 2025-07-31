import React, { PropsWithChildren } from 'react';
import { useTheme } from 'styled-components';
import AlertIcon from 'components/common/Icons/AlertIcon';

import * as S from './AlertBadge.styled';

interface AlertBadgeProps {}
function AlertBadge({ children }: PropsWithChildren<AlertBadgeProps>) {
  return <S.Container role="alert">{children}</S.Container>;
}

const Icon = () => {
  const theme = useTheme();
  return <AlertIcon fill={theme.alertBadge.icon.color} />;
};

interface AlertBadgeContentProps {
  content: string | number;
}
const Content = ({ content }: AlertBadgeContentProps) => {
  return <S.Content>{content}</S.Content>;
};

AlertBadge.Icon = Icon;
AlertBadge.Content = Content;
export default AlertBadge;
