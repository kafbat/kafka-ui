import React from 'react';
import { NavLink } from 'react-router-dom';
import styled, { css } from 'styled-components';

interface LinkCellProps {
  value: string;
  to?: string;
  wordBreak?: boolean;
}

const NavLinkStyled = styled(NavLink)<{ $wordBreak?: boolean }>`
  && {
    ${({ $wordBreak }) =>
      $wordBreak &&
      css`
        word-break: break-word;
        white-space: pre-wrap;
      `}
  }
`;

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

export default LinkCell;
