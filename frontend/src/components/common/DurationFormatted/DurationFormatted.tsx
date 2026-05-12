import React from 'react';

import { formatDuration } from './utils';
import { NoWrap } from './DurationFormatted.styled';

interface Props {
  value: string | number | undefined;
}

const DurationFormatted: React.FC<Props> = ({ value }) => {
  const formattedValue = React.useMemo(() => formatDuration(value), [value]);

  return <NoWrap>{formattedValue}</NoWrap>;
};

export default DurationFormatted;
