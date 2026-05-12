import React from 'react';
import Table from 'components/common/NewTable';
import { RouteParamsClusterTopic } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { useTopicConfig } from 'lib/hooks/api/topics';
import { CellContext, ColumnDef } from '@tanstack/react-table';
import { TopicConfig } from 'generated-sources';
import { getConfigUnit } from 'components/Brokers/Broker/Configs/lib/utils';
import { formatDuration } from 'components/common/DurationFormatted/utils';
import { formatBytes } from 'components/common/BytesFormatted/utils';

import * as S from './Settings.styled';

const getFormattedValue = (
  value: string,
  unit: 'ms' | 'bytes' | undefined
): string | null => {
  if (!unit) return null;

  const numValue = parseInt(value, 10);
  if (Number.isNaN(numValue)) return null;

  if (unit === 'ms') {
    // Don't show formatted for negative values (they mean unbounded)
    if (numValue < 0) return null;
    return formatDuration(numValue);
  }

  if (unit === 'bytes') {
    if (numValue <= 0) return null;
    return formatBytes(numValue);
  }

  return null;
};

const KeyCell: React.FC<CellContext<TopicConfig, unknown>> = ({
  row,
  renderValue,
}) => {
  const { defaultValue, value } = row.original;
  const hasCustomValue = !!defaultValue && value !== defaultValue;

  return (
    <S.Value $hasCustomValue={hasCustomValue}>{renderValue<string>()}</S.Value>
  );
};

const ValueCell: React.FC<CellContext<TopicConfig, unknown>> = ({ row }) => {
  const { name, value, defaultValue, isSensitive } = row.original;
  const hasCustomValue = !!defaultValue && value !== defaultValue;

  if (isSensitive) {
    return (
      <S.Value $hasCustomValue={hasCustomValue} title="Sensitive Value">
        **********
      </S.Value>
    );
  }

  const unit = getConfigUnit(name);
  const formattedValue = getFormattedValue(value ?? '', unit);

  return (
    <S.ValueWrapper>
      <S.Value $hasCustomValue={hasCustomValue}>{value}</S.Value>
      {formattedValue && <S.FormattedValue>{formattedValue}</S.FormattedValue>}
    </S.ValueWrapper>
  );
};

const DefaultValueCell: React.FC<CellContext<TopicConfig, unknown>> = ({
  row,
}) => {
  const { name, value, defaultValue, isSensitive } = row.original;
  const hasCustomValue = !!defaultValue && value !== defaultValue;

  if (!hasCustomValue) {
    return <S.DefaultValue />;
  }

  if (isSensitive) {
    return <S.DefaultValue title="Sensitive Value">**********</S.DefaultValue>;
  }

  const unit = getConfigUnit(name);
  const formattedValue = getFormattedValue(defaultValue ?? '', unit);

  return (
    <S.ValueWrapper>
      <S.DefaultValue>{defaultValue}</S.DefaultValue>
      {formattedValue && <S.FormattedValue>{formattedValue}</S.FormattedValue>}
    </S.ValueWrapper>
  );
};

const Settings: React.FC = () => {
  const props = useAppParams<RouteParamsClusterTopic>();
  const { data = [] } = useTopicConfig(props);

  const columns = React.useMemo<ColumnDef<TopicConfig>[]>(
    () => [
      {
        header: 'Key',
        accessorKey: 'name',
        cell: KeyCell,
      },
      {
        header: 'Value',
        accessorKey: 'value',
        cell: ValueCell,
      },
      {
        header: 'Default Value',
        accessorKey: 'defaultValue',
        cell: DefaultValueCell,
      },
    ],
    []
  );

  return <Table columns={columns} data={data} />;
};

export default Settings;
