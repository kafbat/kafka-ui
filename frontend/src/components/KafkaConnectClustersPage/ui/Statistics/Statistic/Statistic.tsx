import React from 'react';
import AlertBadge from 'components/common/AlertBadge/AlertBadge';

import * as S from './Statistic.styled';

type Props = {
  title: string;
  count: number;
  warningCount?: number;
};
const Statistic = ({ title, count, warningCount }: Props) => {
  return (
    <S.Container>
      <S.Header>{title}</S.Header>
      <S.Footer>
        <S.Count>{count}</S.Count>
        {warningCount && (
          <AlertBadge>
            <AlertBadge.Icon />
            <AlertBadge.Content content={warningCount} />
          </AlertBadge>
        )}
      </S.Footer>
    </S.Container>
  );
};

export default Statistic;
