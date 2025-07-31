import { ColumnSizingState } from '@tanstack/react-table';
import React from 'react';

export type ColumnSizingPersister = {
  columnSizing: ColumnSizingState;
  setColumnSizing: React.Dispatch<React.SetStateAction<ColumnSizingState>>;
};
