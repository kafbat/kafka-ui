import React, { type FC, useState } from 'react';
import { useConfirm } from 'lib/hooks/useConfirm';
import { type CellContext } from '@tanstack/react-table';
import { type BrokerConfig } from 'generated-sources';
import {
  BrokerConfigsTableRow,
  UpdateBrokerConfigCallback,
} from 'components/Brokers/Broker/Configs/lib/types';

import InputCellViewMode from './InputCellViewMode';
import InputCellEditMode from './InputCellEditMode';

interface InputCellProps
  extends CellContext<BrokerConfigsTableRow, BrokerConfig['value']> {
  onUpdate: UpdateBrokerConfigCallback;
}

const InputCell: FC<InputCellProps> = ({ row, getValue, onUpdate }) => {
  const initialValue = getValue();
  const [isEdit, setIsEdit] = useState(false);
  const confirm = useConfirm();

  const handleSave = (newValue: string) => {
    if (newValue !== initialValue) {
      confirm('Are you sure you want to change the value?', () =>
        onUpdate(row?.original?.name, newValue)
      );
    }
    setIsEdit(false);
  };

  const isDynamic = row?.original?.source === 'DYNAMIC_BROKER_CONFIG';

  return isEdit ? (
    <InputCellEditMode
      initialValue={initialValue}
      onSave={handleSave}
      onCancel={() => setIsEdit(false)}
    />
  ) : (
    <InputCellViewMode
      value={initialValue}
      onEdit={() => setIsEdit(true)}
      isDynamic={isDynamic}
    />
  );
};

export default InputCell;
