import { CellContext } from '@tanstack/react-table';
import React from 'react';
import getTagColor from 'components/common/Tag/getTagColor';
import { MultiLineTag } from 'components/common/Tag/Tag.styled';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const MultiLineTagCell: React.FC<CellContext<any, unknown>> = ({
  getValue,
}) => {
  const value = getValue<string>();
  return <MultiLineTag color={getTagColor(value)}>{value}</MultiLineTag>;
};

export default MultiLineTagCell;
