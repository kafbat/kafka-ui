import React from 'react';
import { type BrokerConfig, ConfigSource } from 'generated-sources';
import { createColumnHelper } from '@tanstack/react-table';
import * as BrokerConfigTableComponents from 'components/Brokers/Broker/Configs/TableComponents/index';

import type {
  BrokerConfigsTableRow,
  ConfigUnit,
  UpdateBrokerConfigCallback,
} from './types';
import { CONFIG_SOURCE_NAME_MAP, CONFIG_SOURCE_PRIORITY } from './constants';

const getConfigFieldMatch = (field: string, query: string) =>
  field.toLocaleLowerCase().includes(query.toLocaleLowerCase());

const filterConfigsBySearchQuery =
  (searchQuery: string) => (config: BrokerConfig) => {
    const nameMatch = getConfigFieldMatch(config.name, searchQuery);
    const valueMatch =
      config.value && getConfigFieldMatch(config.value, searchQuery);

    return nameMatch ? true : valueMatch;
  };

const getConfigSourcePriority = (source: ConfigSource): number =>
  CONFIG_SOURCE_PRIORITY[source];

const sortBrokersBySource = (a: BrokerConfig, b: BrokerConfig) => {
  const priorityA = getConfigSourcePriority(a.source);
  const priorityB = getConfigSourcePriority(b.source);

  return priorityA - priorityB;
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
  const found = Object.entries(unitPatterns).find(([, pattern]) =>
    pattern.test(configName)
  );

  return found ? (found[0] as ConfigUnit) : undefined;
};
