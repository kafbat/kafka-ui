import React from 'react';
import { CellContext } from '@tanstack/react-table';

import * as S from './Table.styled';

const BreakableTextCell = <T,>({ getValue }: CellContext<T, unknown>) => {
  return <S.BreakableText>{getValue<string>()}</S.BreakableText>;
};

export default BreakableTextCell;
