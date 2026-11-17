import { useState, useCallback } from 'react';
import { TopicMessage } from 'generated-sources';
import { MessageFormData } from 'lib/interfaces/message';

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

    const keySerdeParams: Record<string, string> = {};
    if (message.keyDeserializeProperties?.subjects?.[0]) {
      keySerdeParams.subject = String(
        message.keyDeserializeProperties.subjects[0]
      );
    }
    if (message.keyDeserializeProperties?.messageName) {
      keySerdeParams.messageName = String(
        message.keyDeserializeProperties.messageName
      );
    }
    if (Object.keys(keySerdeParams).length > 0) {
      data.keySerdeParams = keySerdeParams;
    }

    const valueSerdeParams: Record<string, string> = {};
    if (message.valueDeserializeProperties?.subjects?.[0]) {
      valueSerdeParams.subject = String(
        message.valueDeserializeProperties.subjects[0]
      );
    }
    if (message.valueDeserializeProperties?.messageName) {
      valueSerdeParams.messageName = String(
        message.valueDeserializeProperties.messageName
      );
    }
    if (Object.keys(valueSerdeParams).length > 0) {
      data.valueSerdeParams = valueSerdeParams;
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
