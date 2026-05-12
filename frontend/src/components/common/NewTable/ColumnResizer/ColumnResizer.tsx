import { Header, RowData } from '@tanstack/react-table';
import React from 'react';
import styled from 'styled-components';

const ColumnResizerStyled = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  height: 100%;
  width: 5px;
  background: white;
  cursor: col-resize;
  user-select: none;
  touch-action: none;
`;

const ColumnResizer = <TData extends RowData>({
  header,
}: {
  header: Header<TData, unknown>;
}) => {
  return (
    <ColumnResizerStyled
      {...{
        onDoubleClick: () => header.column.resetSize(),
        onMouseDown: header.getResizeHandler(),
        onTouchStart: header.getResizeHandler(),
      }}
    />
  );
};

export default styled(ColumnResizer)``;
