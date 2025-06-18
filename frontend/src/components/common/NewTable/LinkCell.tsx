import React from 'react';
import { NavLink } from 'react-router-dom';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const LinkCell = ({
  value,
  to = '',
  style = {},
}: {
  value: string;
  to?: string;
  style?: React.CSSProperties;
}) => {
  const handleClick: React.MouseEventHandler = (e) => e.stopPropagation();
  return (
    <NavLink to={to} title={value} onClick={handleClick} style={style}>
      {value}
    </NavLink>
  );
};

export default LinkCell;
