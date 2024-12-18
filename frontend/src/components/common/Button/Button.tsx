import React, { type ButtonHTMLAttributes, type FC } from 'react';
import { Link } from 'react-router-dom';
import Spinner from 'components/common/Spinner/Spinner';

import StyledButton, { ButtonProps } from './Button.styled';

export interface Props
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    ButtonProps {
  to?: string | object;
  inProgress?: boolean;
  className?: string;
}

export const Button: FC<Props> = ({
  to,
  children,
  disabled,
  inProgress,
  ...props
}) => {
  if (to) {
    return (
      <Link to={to} className={props.className}>
        <StyledButton disabled={disabled} type="button" {...props}>
          {children}
        </StyledButton>
      </Link>
    );
  }

  return (
    <StyledButton type="button" disabled={disabled || inProgress} {...props}>
      {children}{' '}
      {inProgress ? (
        <Spinner size={16} borderWidth={2} marginLeft={2} emptyBorderColor />
      ) : null}
    </StyledButton>
  );
};
