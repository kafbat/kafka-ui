import React, { type FC } from 'react';
import { Button } from 'components/common/Button/Button';
import EditIcon from 'components/common/Icons/EditIcon';
import type { ConfigUnit } from 'components/Brokers/Broker/Configs/lib/types';
import Tooltip from 'components/common/Tooltip/Tooltip';
import { getConfigDisplayValue } from 'components/Brokers/Broker/Configs/lib/utils';
import ClusterContext from 'components/contexts/ClusterContext';

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
  const { isReadOnly: isClusterReadOnly } = React.useContext(ClusterContext);

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
            disabled={isReadOnly || isClusterReadOnly}
          >
            <EditIcon /> Edit
          </Button>
        }
        showTooltip={isReadOnly || isClusterReadOnly}
        content={
          isClusterReadOnly ? 'Cluster is read-only' : 'Property is read-only'
        }
        placement="left"
      />
    </S.ValueWrapper>
  );
};

export default InputCellViewMode;
