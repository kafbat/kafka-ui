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
