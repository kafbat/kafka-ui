import { type BrokerConfig } from 'generated-sources';

export type BrokerConfigsTableRow = Pick<
  BrokerConfig,
  'name' | 'value' | 'source' | 'isReadOnly' | 'isSensitive'
>;

export type UpdateBrokerConfigCallback = (
  name: BrokerConfig['name'],
  value: BrokerConfig['value']
) => Promise<void>;

export type ConfigUnit = 'ms' | 'bytes';
