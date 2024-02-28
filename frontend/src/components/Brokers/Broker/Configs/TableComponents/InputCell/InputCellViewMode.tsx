import React, { type FC } from 'react';
import { Button } from 'components/common/Button/Button';
import EditIcon from 'components/common/Icons/EditIcon';
import type { ConfigUnit } from 'components/Brokers/Broker/Configs/lib/types';

import * as S from './styled';

interface InputCellViewModeProps {
  value: string;
  unit: ConfigUnit | undefined;
  onEdit: () => void;
  isDynamic: boolean;
  isSensitive: boolean;
}

const InputCellViewMode: FC<InputCellViewModeProps> = ({
  value,
  unit,
  onEdit,
  isDynamic,
  isSensitive,
}) => {
  let displayValue: string;
  let title: string;

  if (isSensitive) {
    displayValue = '**********';
    title = 'Sensitive Value';
  } else {
    displayValue = unit ? `${value} ${unit}` : value;
    title = displayValue;
  }

  return (
    <S.ValueWrapper $isDynamic={isDynamic}>
      <S.Value title={title}>{displayValue}</S.Value>
      <Button
        buttonType="primary"
        buttonSize="S"
        aria-label="editAction"
        onClick={onEdit}
      >
        <EditIcon /> Edit
      </Button>
    </S.ValueWrapper>
  );
};

export default InputCellViewMode;
