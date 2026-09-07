import React, { type ButtonHTMLAttributes, type FC } from 'react';
import { Link } from 'react-router-dom';
import Spinner from 'components/common/Spinner/Spinner';

import StyledButton from './Button.styled';

export interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  to?: string | object;
  inProgress?: boolean;
  className?: string;
  buttonType: 'primary' | 'secondary' | 'danger' | 'text';
  buttonSize: 'S' | 'M' | 'L';
}

export const Button: FC<Props> = ({
  to,
  children,
  disabled,
  inProgress,
  buttonType,
  buttonSize,
  ...props
}) => {
  if (to) {
    return (
      <Link to={to} className={props.className}>
        <StyledButton
          disabled={disabled}
          type="button"
          $buttonType={buttonType}
          $buttonSize={buttonSize}
          {...props}
        >
          {children}
        </StyledButton>
      </Link>
    );
  }

  return (
    <StyledButton
      type="button"
      disabled={disabled || inProgress}
      $buttonType={buttonType}
      $buttonSize={buttonSize}
      {...props}
    >
      {children}{' '}
      {inProgress ? (
        <Spinner
          $size={16}
          $borderWidth={2}
          $marginLeft={2}
          $emptyBorderColor
        />
      ) : null}
    </StyledButton>
  );
};
