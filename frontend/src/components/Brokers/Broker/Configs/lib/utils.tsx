import React from 'react';
import { type BrokerConfig, ConfigSource } from 'generated-sources';
import { createColumnHelper } from '@tanstack/react-table';
import * as BrokerConfigTableComponents from 'components/Brokers/Broker/Configs/TableComponents/index';
import type {
  BrokerConfigsTableRow,
  ConfigUnit,
  UpdateBrokerConfigCallback,
} from 'components/Brokers/Broker/Configs/lib/types';
import { CONFIG_SOURCE_NAME_MAP } from 'components/Brokers/Broker/Configs/lib/constants';

const getConfigFieldMatch = (field: string, query: string) =>
  field.toLocaleLowerCase().includes(query.toLocaleLowerCase());

const filterConfigsBySearchQuery =
  (searchQuery: string) => (config: BrokerConfig) => {
    const nameMatch = getConfigFieldMatch(config.name, searchQuery);
    const valueMatch =
      config.value && getConfigFieldMatch(config.value, searchQuery);

    return nameMatch ? true : valueMatch;
  };

const sortBrokersBySource = (a: BrokerConfig, b: BrokerConfig) => {
  if (a.source === b.source) return 0;
  return a.source === ConfigSource.DYNAMIC_BROKER_CONFIG ? -1 : 1;
};

export const getConfigTableData = (
  configs: BrokerConfig[],
  searchQuery: string
) =>
  configs
    .filter(filterConfigsBySearchQuery(searchQuery))
    .sort(sortBrokersBySource);

export const getBrokerConfigsTableColumns = (
  onUpdateInputCell: UpdateBrokerConfigCallback
) => {
  const columnHelper = createColumnHelper<BrokerConfigsTableRow>();

  return [
    columnHelper.accessor('name', { header: 'Key' }),
    columnHelper.accessor('value', {
      header: 'Value',
      cell: (props) => (
        <BrokerConfigTableComponents.InputCell
          {...props}
          onUpdate={onUpdateInputCell}
        />
      ),
    }),
    columnHelper.accessor('source', {
      header: BrokerConfigTableComponents.ConfigSourceHeader,
      cell: ({ getValue }) => CONFIG_SOURCE_NAME_MAP[getValue()],
    }),
  ];
};

const unitPatterns = {
  ms: /\.ms$/,
  bytes: /\.bytes$/,
};

export const getConfigUnit = (configName: string): ConfigUnit | undefined => {
  const found = Object.entries(unitPatterns).find(([_, pattern]) =>
    pattern.test(configName)
  );

  return found ? (found[0] as ConfigUnit) : undefined;
};
