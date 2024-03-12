import React, { type FC, ReactNode } from 'react';
import { Button } from 'components/common/Button/Button';
import EditIcon from 'components/common/Icons/EditIcon';
import type { ConfigUnit } from 'components/Brokers/Broker/Configs/lib/types';
import Tooltip from 'components/common/Tooltip/Tooltip';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';

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
  let displayValue: ReactNode | string;
  let title: string;

  if (isSensitive) {
    displayValue = '**********';
    title = 'Sensitive Value';
  } else if (unit === 'bytes' && parseInt(value, 10) > 0) {
    displayValue = <BytesFormatted value={parseInt(value, 10)} />;
    title = `Bytes: ${value}`;
  } else {
    displayValue = unit ? `${value} ${unit}` : value;
    title = displayValue.toString();
  }

  return (
    <S.ValueWrapper $isDynamic={isDynamic}>
      <S.Value title={title}>{displayValue}</S.Value>
      <Tooltip
        value={
          <Button
            buttonType="primary"
            buttonSize="S"
            aria-label="editAction"
            onClick={onEdit}
            disabled={isReadOnly}
          >
            <EditIcon /> Edit
          </Button>
        }
        showTooltip={isReadOnly}
        content="Property is read-only"
        placement="left"
      />
    </S.ValueWrapper>
  );
};

export default InputCellViewMode;
