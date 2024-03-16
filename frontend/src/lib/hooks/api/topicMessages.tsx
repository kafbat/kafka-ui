import React, { useCallback, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { BASE_PARAMS, MESSAGES_PER_PAGE } from 'lib/constants';
import { ClusterName, TopicName } from 'redux/interfaces';
import {
  GetSerdesRequest,
  PollingMode,
  TopicMessage,
  TopicMessageConsuming,
  TopicMessageEvent,
  TopicMessageEventTypeEnum,
} from 'generated-sources';
import { showServerError } from 'lib/errorHandling';
import { useMutation, useQuery } from '@tanstack/react-query';
import { messagesApiClient } from 'lib/api';
import { useSearchParams } from 'react-router-dom';
import { MessagesFilterKeys } from 'lib/hooks/useMessagesFilters';
import { convertStrToPollingMode } from 'lib/hooks/filterUtils';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';

interface UseTopicMessagesProps {
  clusterName: ClusterName;
  topicName: TopicName;
}

export const useTopicMessages = ({
  clusterName,
  topicName,
}: UseTopicMessagesProps) => {
  const [searchParams] = useSearchParams();
  const [messages, setMessages] = React.useState<TopicMessage[]>([]);
  const [phase, setPhase] = React.useState<string>();
  const [consumptionStats, setConsumptionStats] =
    React.useState<TopicMessageConsuming>();
  const [isFetching, setIsFetching] = React.useState(false);
  const abortController = useRef(new AbortController());
  const prevReqUrl = useRef<string>('');

  // get initial properties

  const abortFetchData = useCallback(() => {
    if (abortController.current.signal.aborted) return;

    setIsFetching(false);
    abortController.current.abort();
    abortController.current = new AbortController();
  }, []);

  React.useEffect(() => {
    const mode = convertStrToPollingMode(
      searchParams.get(MessagesFilterKeys.mode) || ''
    );

    const fetchData = async () => {
      setIsFetching(true);

      const url = `${BASE_PARAMS.basePath}/api/clusters/${encodeURIComponent(
        clusterName
      )}/topics/${topicName}/messages/v2`;

      const requestParams = new URLSearchParams({
        limit: searchParams.get(MessagesFilterKeys.limit) || MESSAGES_PER_PAGE,
        mode: searchParams.get(MessagesFilterKeys.mode) || '',
      });

      [
        MessagesFilterKeys.stringFilter,
        MessagesFilterKeys.keySerde,
        MessagesFilterKeys.smartFilterId,
        MessagesFilterKeys.valueSerde,
      ].forEach((item) => {
        const value = searchParams.get(item);
        if (value) {
          requestParams.set(item, value);
        }
      });

      switch (mode) {
        case PollingMode.TO_TIMESTAMP:
        case PollingMode.FROM_TIMESTAMP:
          requestParams.set(
            MessagesFilterKeys.timestamp,
            searchParams.get(MessagesFilterKeys.timestamp) || '0'
          );
          break;
        case PollingMode.TO_OFFSET:
        case PollingMode.FROM_OFFSET:
          requestParams.set(
            MessagesFilterKeys.offset,
            searchParams.get(MessagesFilterKeys.offset) || '0'
          );
          break;
        default:
      }

      searchParams.getAll(MessagesFilterKeys.partitions).forEach((value) => {
        requestParams.append(MessagesFilterKeys.partitions, value);
      });

      const { nextCursor, setNextCursor } = useMessageFiltersStore.getState();

      const tempCompareUrl = new URLSearchParams(requestParams);
      tempCompareUrl.delete(MessagesFilterKeys.cursor);

      const tempToString = tempCompareUrl.toString();

      // filters stay the say and we have cursor set cursor
      if (nextCursor && tempToString === prevReqUrl.current) {
        requestParams.set(MessagesFilterKeys.cursor, nextCursor);
      }

      prevReqUrl.current = tempToString;
      await fetchEventSource(`${url}?${requestParams.toString()}`, {
        method: 'GET',
        signal: abortController.current.signal,
        openWhenHidden: true,
        async onopen(response) {
          const { ok, status } = response;
          if (ok && status === 200) {
            // Reset list of messages.
            setMessages([]);
          } else if (status >= 400 && status < 500 && status !== 429) {
            showServerError(response);
          }
        },
        onmessage(event) {
          const parsedData: TopicMessageEvent = JSON.parse(event.data);
          const { message, consuming, cursor } = parsedData;

          if (useMessageFiltersStore.getState().nextCursor !== cursor?.id) {
            setNextCursor(cursor?.id || undefined);
          }

          switch (parsedData.type) {
            case TopicMessageEventTypeEnum.MESSAGE:
              if (message) {
                setMessages((prevMessages) => {
                  if (mode === PollingMode.TAILING) {
                    return [message, ...prevMessages];
                  }
                  return [...prevMessages, message];
                });
              }
              break;
            case TopicMessageEventTypeEnum.PHASE:
              if (parsedData.phase?.name) setPhase(parsedData.phase.name);
              break;
            case TopicMessageEventTypeEnum.CONSUMING:
              if (consuming) setConsumptionStats(consuming);
              break;
            default:
          }
        },
        onclose() {
          setIsFetching(false);
          abortController.current = new AbortController();
        },
        onerror(err) {
          setNextCursor(undefined);
          setIsFetching(false);
          abortController.current = new AbortController();
          showServerError(err);
        },
      });
    };

    abortFetchData();
    fetchData();

    return abortFetchData;
  }, [searchParams, abortFetchData]);

  return {
    phase,
    messages,
    consumptionStats,
    isFetching,
    abortFetchData,
  };
};

export function useSerdes(props: GetSerdesRequest) {
  const { clusterName, topicName, use } = props;

  return useQuery(
    ['clusters', clusterName, 'topics', topicName, 'serdes', use],
    () => messagesApiClient.getSerdes(props),
    {
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      refetchInterval: false,
    }
  );
}

export function useRegisterSmartFilter({
  clusterName,
  topicName,
}: {
  clusterName: ClusterName;
  topicName: TopicName;
}) {
  return useMutation((payload: { filterCode: string }) => {
    return messagesApiClient.registerFilter({
      clusterName,
      topicName,
      messageFilterRegistration: { filterCode: payload.filterCode },
    });
  });
}
