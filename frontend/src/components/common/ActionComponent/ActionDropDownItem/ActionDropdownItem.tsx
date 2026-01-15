import React from 'react';
import * as S from 'components/common/ActionComponent/ActionComponent.styled';
import {
  ActionComponentProps,
  getDefaultActionMessage,
} from 'components/common/ActionComponent/ActionComponent';
import { useActionTooltip } from 'lib/hooks/useActionTooltip';
import { usePermission } from 'lib/hooks/usePermission';
import { DropdownItemProps } from 'components/common/Dropdown/DropdownItem';
import { DropdownItem } from 'components/common/Dropdown';

interface Props extends ActionComponentProps, DropdownItemProps {
  fallbackPermission?: ActionComponentProps['permission'];
}

const ActionDropdownItem: React.FC<Props> = ({
  permission,
  fallbackPermission,
  message = getDefaultActionMessage(),
  placement = 'left',
  children,
  disabled,
  ...props
}) => {
  const canDoAction = usePermission(
    permission.resource,
    permission.action,
    permission.value
  );

  // Only check fallback if it's provided - use primary values as placeholders otherwise
  // (will result in same permission check, effectively a no-op for the OR logic)
  const canDoFallbackAction = usePermission(
    fallbackPermission?.resource ?? permission.resource,
    fallbackPermission?.action ?? permission.action,
    fallbackPermission?.value ?? permission.value
  );

  const hasPermission =
    canDoAction || (fallbackPermission && canDoFallbackAction);
  const isDisabled = !hasPermission;

  const { x, y, refs, strategy, open } = useActionTooltip(
    isDisabled,
    placement
  );

  return (
    <>
      <DropdownItem
        {...props}
        disabled={disabled || isDisabled}
        ref={refs.setReference}
      >
        {children}
      </DropdownItem>
      {open && (
        <S.MessageTooltip
          ref={refs.setFloating}
          style={{
            position: strategy,
            top: y ?? 0,
            left: x ?? 0,
          }}
        >
          {message}
        </S.MessageTooltip>
      )}
    </>
  );
};

export default ActionDropdownItem;
