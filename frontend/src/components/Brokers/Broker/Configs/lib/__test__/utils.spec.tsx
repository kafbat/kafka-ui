import {
  getConfigTableData,
  getConfigUnit,
} from 'components/Brokers/Broker/Configs/lib/utils';
import { ConfigSource } from 'generated-sources';

describe('getConfigTableData', () => {
  it('filters configs by search query and sorts by source priority', () => {
    const configs = [
      {
        name: 'log.retention.ms',
        value: '7200000',
        source: ConfigSource.DEFAULT_CONFIG,
        isSensitive: true,
        isReadOnly: false,
      },
      {
        name: 'log.segment.bytes',
        value: '1073741824',
        source: ConfigSource.DYNAMIC_BROKER_CONFIG,
        isSensitive: false,
        isReadOnly: true,
      },
      {
        name: 'compression.type',
        value: 'producer',
        source: ConfigSource.DEFAULT_CONFIG,
        isSensitive: true,
        isReadOnly: false,
      },
    ];
    const searchQuery = 'log';
    const result = getConfigTableData(configs, searchQuery);

    expect(result).toHaveLength(2);
    expect(result[0].name).toBe('log.segment.bytes');
    expect(result[1].name).toBe('log.retention.ms');
  });
});

describe('getConfigUnit', () => {
  it('identifies the unit of a configuration name', () => {
    expect(getConfigUnit('log.retention.ms')).toBe('ms');
    expect(getConfigUnit('log.segment.bytes')).toBe('bytes');
    expect(getConfigUnit('compression.type')).toBeUndefined();
  });
});
