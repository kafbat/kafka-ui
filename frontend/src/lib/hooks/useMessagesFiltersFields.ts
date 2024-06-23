import { PollingMode } from 'generated-sources';
import { MessagesFilterKeysTypes } from 'lib/types';
import { MessagesFilterKeys } from 'lib/constants';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';

type MessagesFilterFieldsType = Pick<
  Partial<{
    [key in MessagesFilterKeysTypes]: string;
  }>,
  | 'mode'
  | 'offset'
  | 'timestamp'
  | 'partitions'
  | 'keySerde'
  | 'valueSerde'
  | 'stringFilter'
>;

export function useMessagesFiltersFields(topicName: string) {
  const [messageFilters, setMessageFilters] = useLocalStorage<{
    [topicName: string]: MessagesFilterFieldsType;
  }>('message-filters-fields', {});

  const removeMessagesFilterFields = () => {
    setMessageFilters((prev) => {
      const { [topicName]: topicFilters, ...rest } = prev || {};
      return rest;
    });
  };

  const removeMessagesFiltersField = (key: keyof MessagesFilterFieldsType) => {
    setMessageFilters((prev) => {
      const { [key]: value, ...rest } = prev[topicName] || {};
      return { ...prev, [topicName]: rest };
    });
  };

  const setMessagesFiltersField = (
    key: keyof MessagesFilterFieldsType,
    value: string
  ) => {
    setMessageFilters((prev) => ({
      ...prev,
      [topicName]: { ...prev[topicName], [key]: value },
    }));
  };

  const initMessagesFiltersFields = (params: URLSearchParams) => {
    const topicMessagesFilters = messageFilters[topicName];
    if (params.size === 0 && !!topicMessagesFilters) {
      if (topicMessagesFilters.mode) {
        params.set(MessagesFilterKeys.mode, topicMessagesFilters.mode);
        if (
          topicMessagesFilters.mode === PollingMode.FROM_OFFSET ||
          topicMessagesFilters.mode === PollingMode.TO_OFFSET
        ) {
          if (topicMessagesFilters.offset) {
            params.set(MessagesFilterKeys.offset, topicMessagesFilters.offset);
          }
        }

        if (
          topicMessagesFilters.mode === PollingMode.FROM_TIMESTAMP ||
          topicMessagesFilters.mode === PollingMode.TO_TIMESTAMP
        ) {
          if (topicMessagesFilters.timestamp) {
            params.set(
              MessagesFilterKeys.timestamp,
              topicMessagesFilters.timestamp
            );
          }
        }
      }
      if (topicMessagesFilters.partitions) {
        params.set(
          MessagesFilterKeys.partitions,
          topicMessagesFilters.partitions
        );
      }
      if (topicMessagesFilters.keySerde) {
        params.set(MessagesFilterKeys.keySerde, topicMessagesFilters.keySerde);
      }
      if (topicMessagesFilters.valueSerde) {
        params.set(
          MessagesFilterKeys.valueSerde,
          topicMessagesFilters.valueSerde
        );
      }
      if (topicMessagesFilters.stringFilter) {
        params.set(
          MessagesFilterKeys.stringFilter,
          topicMessagesFilters.stringFilter
        );
      }
    } else {
      const MessagesFiltersMode = params.get(MessagesFilterKeys.mode);
      if (MessagesFiltersMode) {
        removeMessagesFilterFields();
        setMessagesFiltersField(MessagesFilterKeys.mode, MessagesFiltersMode);
        const MessagesFiltersOffset = params.get(MessagesFilterKeys.offset);
        if (MessagesFiltersOffset) {
          setMessagesFiltersField(
            MessagesFilterKeys.offset,
            MessagesFiltersOffset
          );
        }
        const MessagesFiltersTimestamp = params.get(
          MessagesFilterKeys.timestamp
        );
        if (MessagesFiltersTimestamp) {
          setMessagesFiltersField(
            MessagesFilterKeys.timestamp,
            MessagesFiltersTimestamp
          );
        }
      }

      const MessageFiltersPartitions = params.get(
        MessagesFilterKeys.partitions
      );
      if (MessageFiltersPartitions) {
        setMessagesFiltersField(
          MessagesFilterKeys.partitions,
          MessageFiltersPartitions
        );
      }
      const MessagesFiltersKeySerde = params.get(MessagesFilterKeys.keySerde);
      if (MessagesFiltersKeySerde) {
        setMessagesFiltersField(
          MessagesFilterKeys.keySerde,
          MessagesFiltersKeySerde
        );
      }
      const MessagesFiltersValueSerde = params.get(
        MessagesFilterKeys.valueSerde
      );
      if (MessagesFiltersValueSerde) {
        setMessagesFiltersField(
          MessagesFilterKeys.valueSerde,
          MessagesFiltersValueSerde
        );
      }
      const MessagesFiltersStringFilter = params.get(
        MessagesFilterKeys.stringFilter
      );
      if (MessagesFiltersStringFilter) {
        setMessagesFiltersField(
          MessagesFilterKeys.stringFilter,
          MessagesFiltersStringFilter
        );
      }
    }
  };

  return {
    initMessagesFiltersFields,
    removeMessagesFiltersField,
    setMessagesFiltersField,
  };
}
