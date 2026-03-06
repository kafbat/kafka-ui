import { renderHook, act } from '@testing-library/react';
import { useProduceMessage } from 'lib/hooks/useProduceMessage';
import { TopicMessage, TopicMessageTimestampTypeEnum } from 'generated-sources';

describe('useProduceMessage', () => {
  const mockMessage: TopicMessage = {
    timestamp: new Date('2025-07-21T01:30:00Z'),
    timestampType: TopicMessageTimestampTypeEnum.CREATE_TIME,
    offset: 123,
    key: 'test-key',
    keySize: 8,
    partition: 2,
    value: 'test-value',
    valueSize: 10,
    headers: { header1: 'value1', header2: 'value2' },
    valueSerde: 'String',
    keySerde: 'String',
  };

  it('should initialize with null message data', () => {
    const { result } = renderHook(() => useProduceMessage());
    expect(result.current.messageData).toBeNull();
  });

  it('should convert message to message form data correctly', () => {
    const { result } = renderHook(() => useProduceMessage());

    act(() => {
      result.current.setMessage(mockMessage);
    });

    expect(result.current.messageData).toEqual({
      content: 'test-value',
      keepContents: false,
      key: 'test-key',
      headers: '{\n  "header1": "value1",\n  "header2": "value2"\n}',
      partition: 2,
      valueSerde: 'String',
      keySerde: 'String',
    });
  });

  it('should handle message with empty headers', () => {
    const { result } = renderHook(() => useProduceMessage());
    const messageWithEmptyHeaders = { ...mockMessage, headers: {} };

    act(() => {
      result.current.setMessage(messageWithEmptyHeaders);
    });

    expect(result.current.messageData).not.toHaveProperty('headers');
  });

  it('should handle message with missing optional fields', () => {
    const { result } = renderHook(() => useProduceMessage());
    const minimalMessage: TopicMessage = {
      timestamp: new Date('2025-07-20T10:00:00Z'),
      timestampType: TopicMessageTimestampTypeEnum.CREATE_TIME,
      offset: 123,
      partition: 0,
      value: 'value',
    };

    act(() => {
      result.current.setMessage(minimalMessage);
    });

    expect(result.current.messageData).toEqual({
      keepContents: false,
      content: 'value',
      partition: 0,
    });
  });

  it('should extract subjects from deserialize properties', () => {
    const { result } = renderHook(() => useProduceMessage());
    const messageWithSubjects: TopicMessage = {
      ...mockMessage,
      valueSerde: 'SchemaRegistry',
      keySerde: 'SchemaRegistry',
      valueDeserializeProperties: {
        subjects: ['test-topic-value'],
        type: 'AVRO',
        id: 1,
      },
      keyDeserializeProperties: {
        subjects: ['test-topic-key'],
        type: 'AVRO',
        id: 2,
      },
    };

    act(() => {
      result.current.setMessage(messageWithSubjects);
    });

    expect(result.current.messageData?.valueSubject).toBe('test-topic-value');
    expect(result.current.messageData?.keySubject).toBe('test-topic-key');
  });

  it('should handle empty subjects array in deserialize properties', () => {
    const { result } = renderHook(() => useProduceMessage());
    const messageWithEmptySubjects: TopicMessage = {
      ...mockMessage,
      valueSerde: 'SchemaRegistry',
      valueDeserializeProperties: { subjects: [], type: 'AVRO', id: 1 },
    };

    act(() => {
      result.current.setMessage(messageWithEmptySubjects);
    });

    expect(result.current.messageData).not.toHaveProperty('valueSubject');
  });

  it('should clear message on callback', () => {
    const { result } = renderHook(() => useProduceMessage());

    act(() => {
      result.current.setMessage(mockMessage);
    });
    expect(result.current.messageData).not.toBeNull();

    act(() => {
      result.current.clearMessage();
    });
    expect(result.current.messageData).toBeNull();
  });
});
