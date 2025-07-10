import React, { ReactElement } from 'react';
import {
  ActionComponentProps,
  getDefaultActionMessage,
} from 'components/common/ActionComponent/ActionComponent';
import { usePermission } from 'lib/hooks/usePermission';
import { useActionTooltip } from 'lib/hooks/useActionTooltip';
import * as S from 'components/common/ActionComponent/ActionComponent.styled';

interface Props extends ActionComponentProps {
  children: ReactElement;
  onAction: () => void;
}

const ActionPermissionWrapper: React.FC<Props> = ({
  permission,
  onAction,
  children,
  placement,
  message = getDefaultActionMessage(),
}) => {
  const canDoAction = usePermission(
    permission.resource,
    permission.action,
    permission.value
  );

  const { x, y, refs, strategy, open } = useActionTooltip(
    !canDoAction,
    placement
  );

  return (
    <S.Wrapper
      ref={refs.setReference}
      onClick={() => canDoAction && onAction()}
      style={{ cursor: canDoAction ? 'pointer' : 'not-allowed' }}
    >
      {children}
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
    </S.Wrapper>
  );
};

export default ActionPermissionWrapper;
