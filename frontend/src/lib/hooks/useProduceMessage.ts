import { useState, useCallback } from 'react';
import { TopicMessage } from 'generated-sources';
import { MessageFormData } from 'lib/interfaces/message';

const extractSerdeParams = (
  props?: Record<string, unknown>
): Record<string, string> | undefined => {
  if (!props || Object.keys(props).length === 0) return undefined;
  const result = Object.entries(props).reduce<Record<string, string>>(
    (acc, [key, value]) => {
      if (Array.isArray(value) && value.length > 0) {
        acc[key] = String(value[0]);
      } else if (value != null && !Array.isArray(value)) {
        acc[key] = String(value);
      }
      return acc;
    },
    {}
  );
  return Object.keys(result).length > 0 ? result : undefined;
};

interface UseProduceMessageReturn {
  messageData: Partial<MessageFormData> | null;
  setMessage: (message: TopicMessage) => void;
  clearMessage: () => void;
}

export const useProduceMessage = (): UseProduceMessageReturn => {
  const [messageData, setMessageData] =
    useState<Partial<MessageFormData> | null>(null);

  const setMessage = useCallback((message: TopicMessage) => {
    const data: Partial<MessageFormData> = {
      keepContents: false,
      content: message.value || '',
    };

    if (message.key) {
      data.key = message.key;
    }

    if (message.headers && Object.keys(message.headers).length > 0) {
      data.headers = JSON.stringify(message.headers, null, 2);
    }

    if (message.partition !== undefined) {
      data.partition = message.partition;
    }

    if (message.valueSerde) {
      data.valueSerde = message.valueSerde;
    }

    if (message.keySerde) {
      data.keySerde = message.keySerde;
    }

    const keyParams = extractSerdeParams(message.keyDeserializeProperties);
    if (keyParams) {
      data.keySerdeParams = keyParams;
    }

    const valueParams = extractSerdeParams(message.valueDeserializeProperties);
    if (valueParams) {
      data.valueSerdeParams = valueParams;
    }

    setMessageData(data);
  }, []);

  const clearMessage = useCallback(() => {
    setMessageData(null);
  }, []);

  return {
    messageData,
    setMessage,
    clearMessage,
  };
};
