import styled from 'styled-components';

interface Props {
  isFullwidth?: boolean;
}

export const Table = styled.table<Props>`
  width: ${(props) => (props.isFullwidth ? '100%' : 'auto')};
  background: ${({ theme }) => theme.surface.panel};
  border: 1px solid ${({ theme }) => theme.surface.border};
  border-radius: 8px;
  border-collapse: separate;
  border-spacing: 0;
  overflow: hidden;
  box-shadow: ${({ theme }) => theme.surface.shadow};

  & td {
    border-top: 1px ${({ theme }) => theme.table.td.borderTop} solid;
    font-size: 14px;
    font-weight: 400;
    padding: 12px 16px;
    color: ${({ theme }) => theme.table.td.color.normal};
    vertical-align: middle;
    max-width: 350px;
    word-wrap: break-word;
    &.break-spaces {
      white-space: break-spaces;
    }
  }

  & tbody > tr {
    &:hover {
      background-color: ${({ theme }) => theme.table.tr.backgroundColor.hover};
    }
  }
`;
