import { CellContext } from '@tanstack/react-table';
import { Connect } from 'generated-sources';
import React from 'react';

type Props = CellContext<Connect, string>;
const NameCell = ({ getValue }: Props) => {
  return <div>{getValue()}</div>;
};
export default NameCell;
