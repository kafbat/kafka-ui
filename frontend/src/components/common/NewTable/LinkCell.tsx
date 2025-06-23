import React from 'react';
import { NavLink } from 'react-router-dom';
import styled, { css } from 'styled-components';

interface LinkCellProps {
  value: string;
  to?: string;
  wordBreak?: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const LinkCell: React.FC<LinkCellProps> = ({
  value,
  to = '',
  wordBreak = false,
}) => {
  const handleClick: React.MouseEventHandler = (e) => e.stopPropagation();
  return (
    <NavLinkStyled
      to={to}
      title={value}
      onClick={handleClick}
      $wordBreak={wordBreak}
    >
      {value}
    </NavLinkStyled>
  );
};

const NavLinkStyled = styled(NavLink)<{ $wordBreak?: boolean }>`
  ${({ $wordBreak }) => {
    if ($wordBreak) {
      return css`
        word-break: break-word !important;
        white-space: pre-wrap !important;
      `;
    }
  }}
`;

export default LinkCell;
