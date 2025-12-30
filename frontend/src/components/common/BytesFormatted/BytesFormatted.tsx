import React from 'react';

import { formatBytes } from './utils';
import { NoWrap } from './BytesFormatted.styled';

interface Props {
  value: string | number | undefined;
  precision?: number;
}

const BytesFormatted: React.FC<Props> = ({ value, precision = 0 }) => {
  const formattedValue = React.useMemo(
    () => formatBytes(value, precision),
    [precision, value]
  );

  return <NoWrap>{formattedValue}</NoWrap>;
};

export default BytesFormatted;
