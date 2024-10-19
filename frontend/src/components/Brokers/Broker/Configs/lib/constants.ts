import { ConfigSource } from 'generated-sources';

export const CONFIG_SOURCE_NAME_MAP: Record<ConfigSource, string> = {
  [ConfigSource.DYNAMIC_TOPIC_CONFIG]: 'Dynamic topic config',
  [ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG]: 'Dynamic broker logger config',
  [ConfigSource.DYNAMIC_BROKER_CONFIG]: 'Dynamic broker config',
  [ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG]: 'Dynamic default broker config',
  [ConfigSource.DYNAMIC_CLIENT_METRICS_CONFIG]: 'Dynamic client metrics config',
  [ConfigSource.STATIC_BROKER_CONFIG]: 'Static broker config',
  [ConfigSource.DEFAULT_CONFIG]: 'Default config',
  [ConfigSource.UNKNOWN]: 'Unknown',
} as const;

export const CONFIG_SOURCE_PRIORITY = {
  [ConfigSource.DYNAMIC_TOPIC_CONFIG]: 1,
  [ConfigSource.DYNAMIC_BROKER_LOGGER_CONFIG]: 1,
  [ConfigSource.DYNAMIC_BROKER_CONFIG]: 1,
  [ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG]: 1,
  [ConfigSource.DYNAMIC_CLIENT_METRICS_CONFIG]: 1,
  [ConfigSource.STATIC_BROKER_CONFIG]: 2,
  [ConfigSource.DEFAULT_CONFIG]: 3,
  [ConfigSource.UNKNOWN]: 4,
  UNHANDLED: 5,
} as const;
