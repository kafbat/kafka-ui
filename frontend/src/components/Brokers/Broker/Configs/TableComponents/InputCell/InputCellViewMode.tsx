import React, { type FC } from 'react';
import EditIcon from 'components/common/Icons/EditIcon';
import type { ConfigUnit } from 'components/Brokers/Broker/Configs/lib/types';
import { getConfigDisplayValue } from 'components/Brokers/Broker/Configs/lib/utils';
import { ActionButton } from 'components/common/ActionComponent';
import { Action, ResourceType } from 'generated-sources';
import { getDefaultActionMessage } from 'components/common/ActionComponent/ActionComponent';

import * as S from './styled';

interface InputCellViewModeProps {
  value: string;
  unit: ConfigUnit | undefined;
  onEdit: () => void;
  isDynamic: boolean;
  isSensitive: boolean;
  isReadOnly: boolean;
}

const InputCellViewMode: FC<InputCellViewModeProps> = ({
  value,
  unit,
  onEdit,
  isDynamic,
  isSensitive,
  isReadOnly,
}) => {
  const { displayValue, title } = getConfigDisplayValue(
    isSensitive,
    value,
    unit
  );

  return (
    <S.ValueWrapper>
      <S.Value $isDynamic={isDynamic} title={title}>
        {displayValue}
      </S.Value>
      <ActionButton
        buttonType="primary"
        buttonSize="S"
        aria-label="editAction"
        onClick={onEdit}
        disabled={isReadOnly}
        message={
          isReadOnly ? 'Property is read-only' : getDefaultActionMessage()
        }
        permission={{
          resource: ResourceType.CLUSTERCONFIG,
          action: Action.EDIT,
        }}
      >
        <EditIcon /> Edit
      </ActionButton>
    </S.ValueWrapper>
  );
};

export default InputCellViewMode;
