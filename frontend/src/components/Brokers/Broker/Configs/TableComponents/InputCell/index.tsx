import React, { type FC, useState } from 'react';
import { useConfirm } from 'lib/hooks/useConfirm';
import { type CellContext } from '@tanstack/react-table';
import { type BrokerConfig } from 'generated-sources';
import {
  BrokerConfigsTableRow,
  UpdateBrokerConfigCallback,
} from 'components/Brokers/Broker/Configs/lib/types';
import { getConfigUnit } from 'components/Brokers/Broker/Configs/lib/utils';

import InputCellViewMode from './InputCellViewMode';
import InputCellEditMode from './InputCellEditMode';

export interface InputCellProps
  extends CellContext<BrokerConfigsTableRow, BrokerConfig['value']> {
  onUpdate: UpdateBrokerConfigCallback;
}

const InputCell: FC<InputCellProps> = ({ row, onUpdate }) => {
  const [isEdit, setIsEdit] = useState(false);
  const confirm = useConfirm();
  const {
    name,
    source,
    value: initialValue,
    isSensitive,
    isReadOnly,
  } = row.original;

  const handleSave = (newValue: string) => {
    if (newValue !== initialValue) {
      confirm('Are you sure you want to change the value?', () =>
        onUpdate(name, newValue)
      );
    }
    setIsEdit(false);
  };

  const isDynamic = source === 'DYNAMIC_BROKER_CONFIG';
  const configUnit = getConfigUnit(name);

  return isEdit ? (
    <InputCellEditMode
      initialValue={String(initialValue)}
      onSave={handleSave}
      onCancel={() => setIsEdit(false)}
    />
  ) : (
    <InputCellViewMode
      unit={configUnit}
      value={String(initialValue)}
      onEdit={() => setIsEdit(true)}
      isDynamic={isDynamic}
      isSensitive={isSensitive}
      isReadOnly={isReadOnly}
    />
  );
};

export default InputCell;
