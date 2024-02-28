import React, { type FC } from 'react';
import { Button } from 'components/common/Button/Button';
import EditIcon from 'components/common/Icons/EditIcon';

import * as S from './styled';

interface InputCellViewModeProps {
  value: string;
  onEdit: () => void;
  isDynamic: boolean;
}

const InputCellViewMode: FC<InputCellViewModeProps> = ({
  value,
  onEdit,
  isDynamic,
}) => (
  <S.ValueWrapper style={{ fontWeight: isDynamic ? 600 : 400 }}>
    <S.Value title={value}>{value}</S.Value>
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

export default InputCellViewMode;
