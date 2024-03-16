import React, { type FC, useState } from 'react';
import Input from 'components/common/Input/Input';
import { Button } from 'components/common/Button/Button';
import CheckmarkIcon from 'components/common/Icons/CheckmarkIcon';
import CancelIcon from 'components/common/Icons/CancelIcon';

import * as S from './styled';

interface EditModeProps {
  initialValue: string;
  onSave: (value: string) => void;
  onCancel: () => void;
}

const InputCellEditMode: FC<EditModeProps> = ({
  initialValue,
  onSave,
  onCancel,
}) => {
  const [value, setValue] = useState(initialValue);

  return (
    <S.ValueWrapper>
      <Input
        type="text"
        inputSize="S"
        value={value}
        aria-label="inputValue"
        onChange={({ target }) => setValue(target.value)}
      />
      <S.ButtonsWrapper>
        <Button
          buttonType="primary"
          buttonSize="S"
          aria-label="confirmAction"
          onClick={() => onSave(value)}
        >
          <CheckmarkIcon /> Save
        </Button>
        <Button
          buttonType="primary"
          buttonSize="S"
          aria-label="cancelAction"
          onClick={onCancel}
        >
          <CancelIcon /> Cancel
        </Button>
      </S.ButtonsWrapper>
    </S.ValueWrapper>
  );
};

export default InputCellEditMode;
