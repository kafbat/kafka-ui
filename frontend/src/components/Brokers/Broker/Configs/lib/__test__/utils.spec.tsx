import {
  getConfigDisplayValue,
  getConfigTableData,
  getConfigUnit,
} from 'components/Brokers/Broker/Configs/lib/utils';
import { ConfigSource } from 'generated-sources';
import { render } from 'lib/testHelpers';
import { ReactElement } from 'react';

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

describe('getConfigDisplayValue', () => {
  it('masks sensitive data with asterisks', () => {
    const result = getConfigDisplayValue(true, 'testValue', undefined);
    expect(result).toEqual({
      displayValue: '**********',
      title: 'Sensitive Value',
    });
  });

  it('returns formatted bytes when unit is "bytes" and value is positive', () => {
    const { container } = render(
      getConfigDisplayValue(false, '1024', 'bytes').displayValue as ReactElement
    );
    expect(container).toHaveTextContent('1 KB');
    expect(getConfigDisplayValue(false, '1024', 'bytes').title).toBe(
      'Bytes: 1024'
    );
  });

  it('returns value as is when unit is "bytes" but value is non-positive', () => {
    const result = getConfigDisplayValue(false, '-1', 'bytes');
    expect(result.displayValue).toBe('-1');
    expect(result.title).toBe('-1');
  });

  it('appends unit to the value when unit is provided and is not "bytes"', () => {
    const result = getConfigDisplayValue(false, '100', 'ms');
    expect(result.displayValue).toBe('100 ms');
    expect(result.title).toBe('100 ms');
  });

  it('returns value as is when no unit is provided', () => {
    const result = getConfigDisplayValue(false, 'testValue', undefined);
    expect(result.displayValue).toBe('testValue');
    expect(result.title).toBe('testValue');
  });
});
