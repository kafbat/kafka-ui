import { ConfigSource } from 'generated-sources';

export const CONFIG_SOURCE_NAME_MAP: Record<ConfigSource, string> = {
  [ConfigSource.DYNAMIC_TOPIC_CONFIG]: 'Dynamic topic config',
  [ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG]: 'Dynamic broker logger config',
  [ConfigSource.DYNAMIC_BROKER_CONFIG]: 'Dynamic broker config',
  [ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG]: 'Dynamic default broker config',
  [ConfigSource.STATIC_BROKER_CONFIG]: 'Static broker config',
  [ConfigSource.DEFAULT_CONFIG]: 'Default config',
  [ConfigSource.UNKNOWN]: 'Unknown',
};
