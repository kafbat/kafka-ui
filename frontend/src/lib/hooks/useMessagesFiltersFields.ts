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
  | 'endTimestamp'
  | 'partitions'
  | 'keySerde'
  | 'valueSerde'
  | 'stringFilter'
  | 'activeFilterId'
  | 'smartFilterId'
>;

export function useMessagesFiltersFields(resourceName: string) {
  const [messageFilters, setMessageFilters] = useLocalStorage<{
    [resourceName: string]: MessagesFilterFieldsType;
  }>('message-filters-fields', {});

  const removeMessagesFilterFields = () => {
    setMessageFilters((prev) => {
      const { [resourceName]: topicFilters, ...rest } = prev || {};
      return rest;
    });
  };

  const removeMessagesFiltersField = (key: keyof MessagesFilterFieldsType) => {
    setMessageFilters((prev) => {
      const { [key]: value, ...rest } = prev[resourceName] || {};
      return { ...prev, [resourceName]: rest };
    });
  };

  const setMessagesFiltersField = (
    key: keyof MessagesFilterFieldsType,
    value: string
  ) => {
    setMessageFilters((prev) => ({
      ...prev,
      [resourceName]: { ...prev[resourceName], [key]: value },
    }));
  };

  const initMessagesFiltersFields = (params: URLSearchParams) => {
    const topicMessagesFilters = messageFilters[resourceName];

    const setTopicMessageFiltersFromLocalStorage = (
      key: keyof MessagesFilterFieldsType
    ) => {
      const value = topicMessagesFilters[key];
      if (value) {
        params.set(MessagesFilterKeys[key], value);
      }
    };

    const setTopicMessageFiltersFromUrlParams = (
      key: keyof MessagesFilterFieldsType
    ) => {
      const filters = params.get(MessagesFilterKeys[key]);
      if (filters) {
        setMessagesFiltersField(key, filters);
      }
    };

    // if url params are empty and topicMessagesFilters from local storage are existing then we apply them
    if (params.size === 0 && !!topicMessagesFilters) {
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.mode);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.offset);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.timestamp);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.endTimestamp);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.partitions);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.keySerde);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.valueSerde);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.stringFilter);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.activeFilterId);
      setTopicMessageFiltersFromLocalStorage(MessagesFilterKeys.smartFilterId);
    } else {
      removeMessagesFilterFields();
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.mode);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.offset);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.timestamp);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.endTimestamp);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.partitions);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.keySerde);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.valueSerde);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.activeFilterId);
      setTopicMessageFiltersFromUrlParams(MessagesFilterKeys.smartFilterId);
    }
  };

  return {
    initMessagesFiltersFields,
    removeMessagesFiltersField,
    setMessagesFiltersField,
  };
}
