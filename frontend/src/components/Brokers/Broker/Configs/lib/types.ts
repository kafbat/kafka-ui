import { type BrokerConfig } from 'generated-sources';

export type BrokerConfigsTableRow = Pick<
  BrokerConfig,
  'name' | 'value' | 'source'
>;

export type UpdateBrokerConfigCallback = (
  name: BrokerConfig['name'],
  value: BrokerConfig['value']
) => Promise<void>;
