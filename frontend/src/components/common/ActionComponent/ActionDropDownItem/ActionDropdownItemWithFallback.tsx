import React from 'react';
import { usePermission } from 'lib/hooks/usePermission';
import { DropdownItemProps } from 'components/common/Dropdown/DropdownItem';
import {
  ActionComponentProps,
  getDefaultActionMessage,
} from 'components/common/ActionComponent/ActionComponent';

import ActionDropdownItem from './ActionDropdownItem';

interface Props
  extends Omit<ActionComponentProps, 'permission'>,
    DropdownItemProps {
  permission:
    | ActionComponentProps['permission']
    | ActionComponentProps['permission'][];
}

/**
 * ActionDropdownItem that supports multiple permission checks.
 * If an array of permissions is provided, it will check them in order
 * and use the first one that grants access.
 */
const ActionDropdownItemWithFallback: React.FC<Props> = ({
  permission,
  message = getDefaultActionMessage(),
  placement = 'left',
  children,
  disabled,
  ...props
}) => {
  const permissions = Array.isArray(permission) ? permission : [permission];

  // Check all permissions upfront to avoid conditional hook calls
  const permissionResults = permissions.map((perm) =>
    // eslint-disable-next-line react-hooks/rules-of-hooks
    usePermission(perm.resource, perm.action, perm.value)
  );

  // Find the first permission that grants access
  let effectivePermission = permissions[0];
  const hasAnyPermission = permissionResults.some((result, index) => {
    if (result) {
      effectivePermission = permissions[index];
      return true;
    }
    return false;
  });

  // If no permissions granted, the ActionDropdownItem will handle hiding
  return (
    <ActionDropdownItem
      {...props}
      permission={effectivePermission}
      message={message}
      placement={placement}
      disabled={disabled || !hasAnyPermission}
    >
      {children}
    </ActionDropdownItem>
  );
};

export default ActionDropdownItemWithFallback;
