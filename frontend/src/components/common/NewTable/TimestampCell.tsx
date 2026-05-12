import { CellContext } from '@tanstack/react-table';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import React from 'react';
import { useTimezone } from 'lib/hooks/useTimezones';

import * as S from './Table.styled';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const TimestampCell: React.FC<CellContext<any, unknown>> = ({ getValue }) => {
  const { currentTimezone } = useTimezone();

  return (
    <S.Nowrap>
      {formatTimestamp({
        timestamp: getValue<string | number>(),
        timezone: currentTimezone.value,
      })}
    </S.Nowrap>
  );
};

export default TimestampCell;
