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
}

const InputCellViewMode: FC<InputCellViewModeProps> = ({
  value,
  unit,
  onEdit,
  isDynamic,
}) => {
  const valueWithUnit = unit ? `${value} ${unit}` : value;

  return (
    <S.ValueWrapper $isDynamic={isDynamic}>
      <S.Value title={valueWithUnit}>{valueWithUnit}</S.Value>
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
