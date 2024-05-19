import React from 'react';
import {
  ActionComponentProps,
  getDefaultActionMessage,
} from 'components/common/ActionComponent/ActionComponent';
import { NavLink, NavLinkProps } from 'react-router-dom';
import * as S from 'components/common/ActionComponent/ActionComponent.styled';
import { useActionTooltip } from 'lib/hooks/useActionTooltip';
import { usePermission } from 'lib/hooks/usePermission';

interface Props extends ActionComponentProps, NavLinkProps {}

const ActionNavLink: React.FC<Props> = ({
  message = getDefaultActionMessage(),
  placement,
  children,
  permission,
  className,
  ...props
}) => {
  const canDoAction = usePermission(
    permission.resource,
    permission.action,
    permission.value
  );

  const isDisabled = !canDoAction;

  const { x, y, refs, strategy, open } = useActionTooltip(
    isDisabled,
    placement
  );

  return (
    <>
      <NavLink
        {...props}
        ref={refs.setReference}
        className={isDisabled ? 'is-disabled' : className}
        aria-disabled={isDisabled}
        onClick={(event) => (isDisabled ? event.preventDefault() : null)}
      >
        {children}
      </NavLink>
      {open && (
        <S.MessageTooltipLimited
          ref={refs.setFloating}
          style={{
            position: strategy,
            top: y ?? 0,
            left: x ?? 0,
            width: 'max-content',
          }}
        >
          {message}
        </S.MessageTooltipLimited>
      )}
    </>
  );
};

export default ActionNavLink;
