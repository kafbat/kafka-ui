import React from 'react';
import AlertBadge from 'components/common/AlertBadge/AlertBadge';
import SpinnerIcon from 'components/common/Icons/SpinnerIcon';

import * as S from './Statistic.styled';

type Props = {
  title: string;
  count: number;
  isLoading: boolean;
  warningCount?: number;
};
const Statistic = ({ title, count, warningCount, isLoading }: Props) => {
  const showWarning = warningCount !== undefined && warningCount > 0;
  return (
    <S.Container role="cell">
      <S.Header>{title}</S.Header>
      <S.Footer>
        {isLoading ? (
          <div role="status">
            <SpinnerIcon />
          </div>
        ) : (
          <>
            <S.Count role="contentinfo">{count}</S.Count>
            {showWarning && (
              <AlertBadge>
                <AlertBadge.Icon />
                <AlertBadge.Content content={warningCount} />
              </AlertBadge>
            )}
          </>
        )}
      </S.Footer>
    </S.Container>
  );
};

export default Statistic;
