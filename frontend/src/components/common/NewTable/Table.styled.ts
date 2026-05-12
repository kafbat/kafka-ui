import styled, { css } from 'styled-components';

export const ExpaderButton = styled.svg<{
  $disabled: boolean;
  getIsExpanded: boolean;
}>(
  ({ theme: { table }, $disabled, getIsExpanded }) => css`
    & > path {
      fill: ${table.expander[
        ($disabled && 'disabled') || (getIsExpanded && 'active') || 'normal'
      ]};
    }
    &:hover > path {
      fill: ${table.expander[$disabled ? 'disabled' : 'hover']};
    }
    &:active > path {
      fill: ${table.expander[$disabled ? 'disabled' : 'active']};
    }
  `
);

interface ThProps {
  sortable?: boolean;
  sortOrder?: 'desc' | 'asc' | false;
  expander?: boolean;
}

const sortableMixin = (normalColor: string, hoverColor: string) => `
  cursor: pointer;
  padding-left: 14px;
  position: relative;

  &::before,
  &::after {
    border: 4px solid transparent;
    content: '';
    display: block;
    height: 0;
    left: 0px;
    top: 50%;
    position: absolute;
  }
  &::before {
    border-bottom-color: ${normalColor};
    margin-top: -9px;
  }
  &::after {
    border-top-color: ${normalColor};
    margin-top: 1px;
  }
  &:hover {
    color: ${hoverColor};
  }
`;

const ASCMixin = (color: string) => `
  color: ${color};
  &:before {
    border-bottom-color: ${color};
  }
  &:after {
    border-top-color: rgba(0, 0, 0, 0.1);
  }
`;
const DESCMixin = (color: string) => `
  color: ${color};
  &:before {
    border-bottom-color: rgba(0, 0, 0, 0.1);
  }
  &:after {
    border-top-color: ${color};
  }
`;

export const TableHeaderContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const ColumnResizer = styled.div<{ $isResizing?: boolean }>`
  ${({
    $isResizing,
    theme: {
      table: { resizer },
    },
  }) => {
    return css`
      opacity: ${$isResizing ? 1 : 0};
      position: absolute;
      top: 4px;
      right: 0;
      height: calc(100% - 8px);
      width: 4px;
      border-radius: 2px;
      background-color: ${resizer.background.normal};

      cursor: col-resize;
      user-select: none;
      touch-action: none;
      &:hover {
        background-color: ${resizer.background.hover};
      }
    `;
  }};
`;

export const Th = styled.th<ThProps>(
  ({
    theme: {
      table: { th },
    },
    sortable,
    sortOrder,
    expander,
  }) => `
  padding: 8px 0 8px 24px;
  border-bottom-width: 1px;
  vertical-align: middle;
  text-align: left;
  font-family: Inter, sans-serif;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
  line-height: 16px;
  letter-spacing: 0em;
  text-align: left;
  background: ${th.backgroundColor.normal};
  width: ${expander ? '5px' : 'auto'};
  white-space: nowrap;
  position: relative;
  user-select: none;

  & > ${TableHeaderContent} {
    cursor: default;
    color: ${th.color.normal};
    ${sortable ? sortableMixin(th.color.sortable, th.color.hover) : ''}
    ${sortable && sortOrder === 'asc' && ASCMixin(th.color.active)}
    ${sortable && sortOrder === 'desc' && DESCMixin(th.color.active)}
  }

  &:hover > ${ColumnResizer} {
    opacity: 1;
  }
`
);

interface RowProps {
  clickable?: boolean;
  expanded?: boolean;
}

export const Row = styled.tr<RowProps>(
  ({ theme: { table }, expanded, clickable }) => css`
    cursor: ${clickable ? 'pointer' : 'default'};
    background-color: ${table.tr.backgroundColor[
      expanded ? 'hover' : 'normal'
    ]};
    & .show-on-hover {
      visibility: hidden;
    }
    &:hover {
      background-color: ${table.tr.backgroundColor.hover};

      & .show-on-hover {
        visibility: visible;
      }
    }
  `
);

export const ExpandedRowInfo = styled.div`
  background-color: ${({ theme }) => theme.table.tr.backgroundColor.normal};
  padding: 24px;
  border-radius: 8px;
  margin: 0 8px 8px 0;
`;

export const Nowrap = styled.div`
  white-space: nowrap;
`;

export const TableActionsBar = styled.div`
  padding: 8px;
  background-color: ${({ theme }) => theme.table.actionBar.backgroundColor};
  margin: 16px 0;
  display: flex;
  gap: 8px;
`;

export const Table = styled.table(
  ({ theme: { table } }) => `
  width: 100%;

  td {
    border-top: 1px ${table.td.borderTop} solid;
    font-size: 14px;
    font-weight: 400;
    padding: 8px 8px 8px 24px;
    color: ${table.td.color.normal};
    vertical-align: middle;
    word-wrap: break-word;
    white-space: pre;

    & a {
      color: ${table.td.color.normal};
      font-weight: 500;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: block;

      &:hover {
        color: ${table.link.color.hover};
      }

      &:active {
        color: ${table.link.color.active};
      }
      &:button {
      color: ${table.link.color.active};
      }

    }
  }
`
);

export const EmptyTableMessageCell = styled.td`
  padding: 16px;
  text-align: center;
`;

export const Pagination = styled.div`
  display: flex;
  justify-content: space-between;
  padding: 16px;
  line-height: 32px;
`;

export const Pages = styled.div`
  display: flex;
  justify-content: left;
  white-space: nowrap;
  flex-wrap: nowrap;
  gap: 8px;
`;

export const GoToPage = styled.label`
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  margin-left: 8px;
  color: ${({ theme }) => theme.table.pagination.info};
`;

export const PageInfo = styled.div`
  display: flex;
  justify-content: right;
  gap: 8px;
  font-size: 14px;
  flex-wrap: nowrap;
  white-space: nowrap;
  margin-left: 16px;
  color: ${({ theme }) => theme.table.pagination.info};
`;

export const Ellipsis = styled.div`
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
`;

export const TableWrapper = styled.div<{ $disabled: boolean }>(
  ({ $disabled }) => css`
    overflow: clip;
    overflow-x: auto;
    overflow-y: visible;
    ${$disabled &&
    css`
      pointer-events: none;
      opacity: 0.5;
    `}
  `
);
