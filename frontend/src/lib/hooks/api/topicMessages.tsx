import React, { useCallback, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import {
  BASE_PARAMS,
  MESSAGES_PER_PAGE,
  MessagesFilterKeys,
} from 'lib/constants';
import {
  GetSerdesRequest,
  PollingMode,
  TopicMessage,
  TopicMessageConsuming,
  TopicMessageEvent,
  TopicMessageEventTypeEnum,
} from 'generated-sources';
import { showServerError } from 'lib/errorHandling';
import { useMutation, useSuspenseQuery } from '@tanstack/react-query';
import { messagesApiClient } from 'lib/api';
import { useSearchParams } from 'react-router-dom';
import { getCursorValue } from 'lib/hooks/useMessagesFilters';
import { convertStrToPollingMode } from 'lib/hooks/filterUtils';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';
import { TopicName } from 'lib/interfaces/topic';
import { ClusterName } from 'lib/interfaces/cluster';

interface UseTopicMessagesProps {
  clusterName: ClusterName;
  topicName: TopicName;
}

interface DownloadMessagesZipProps {
  clusterName: ClusterName;
  topicName: TopicName;
  limit: number;
  partitions?: Array<number | string>;
  stringFilter?: string;
  smartFilterId?: string;
  keySerde?: string;
  valueSerde?: string;
  downloadMode?: string;
  offset?: string;
  timestamp?: string;
  timestampTo?: string;
  format?: string;
}

export interface UploadMessagesFileResult {
  fileName: string;
  extractedEntries: number;
  parsedMessages: number;
}

export interface UploadMessagePreview {
  sourceFile: string;
  entryName: string;
  partition?: number | null;
  key?: string | null;
  valueBytes: number;
  valuePreview: string;
}

export interface UploadMessagesResult {
  dryRun: boolean;
  filesReceived: number;
  entriesRead: number;
  messagesParsed: number;
  messagesProduced: number;
  failures: number;
  files: UploadMessagesFileResult[];
  previews: UploadMessagePreview[];
  errors: string[];
}

interface UploadMessagesProps {
  clusterName: ClusterName;
  topicName: TopicName;
  files: File[];
  parseMode: string;
  partitionStrategy: string;
  keyMode: string;
  partition?: string;
  partitions?: Array<number | string>;
  keySerde?: string;
  valueSerde?: string;
  headersJson?: string;
  includeMetadataHeaders: boolean;
  dryRun: boolean;
  messageLimit?: string;
}

const zipFileNameFromHeader = (contentDisposition: string | null) => {
  if (!contentDisposition) return undefined;

  const utf8FileName = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  const fileName = /filename="?([^";]+)"?/i.exec(contentDisposition);
  const encodedFileName = utf8FileName?.[1] || fileName?.[1];

  if (!encodedFileName) return undefined;

  try {
    return decodeURIComponent(encodedFileName.replace(/"/g, ''));
  } catch {
    return encodedFileName.replace(/"/g, '');
  }
};

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
  const prevCursor = useRef(0);

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

      const partitions = searchParams.get(MessagesFilterKeys.partitions);
      if (partitions !== null) {
        requestParams.append(MessagesFilterKeys.partitions, partitions);
      }
      const { nextCursor, setNextCursor } = useMessageFiltersStore.getState();

      const tempCompareUrl = new URLSearchParams(requestParams);
      tempCompareUrl.delete(MessagesFilterKeys.cursor);

      const currentCursor = getCursorValue(searchParams);

      // filters stay the same and we have cursor set cursor
      if (nextCursor && prevCursor.current < currentCursor) {
        requestParams.set(MessagesFilterKeys.cursor, nextCursor);
      }

      prevCursor.current = currentCursor;

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
          /**
           * abortController.current = new AbortController(); rewrites ref, but fetchEventSource still has old ref
           * that way we cant stop default retry algorythm and stop retry loop
           */
          // abortController.current = new AbortController();
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

  return useSuspenseQuery({
    queryKey: ['clusters', clusterName, 'topics', topicName, 'serdes', use],
    queryFn: () => messagesApiClient.getSerdes(props),
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchInterval: false,
  });
}

export function useDownloadMessagesZip() {
  return useMutation({
    mutationFn: async ({
      clusterName,
      topicName,
      limit,
      partitions,
      stringFilter,
      smartFilterId,
      keySerde,
      valueSerde,
      downloadMode,
      offset,
      timestamp,
      timestampTo,
      format,
    }: DownloadMessagesZipProps) => {
      const requestParams = new URLSearchParams({
        limit: limit.toString(),
      });

      if (partitions?.length) {
        requestParams.set('partitions', partitions.join(','));
      }

      const optionalParams: Array<[string, string | undefined]> = [
        ['stringFilter', stringFilter],
        ['smartFilterId', smartFilterId],
        ['keySerde', keySerde],
        ['valueSerde', valueSerde],
        ['downloadMode', downloadMode],
        ['offset', offset],
        ['timestamp', timestamp],
        ['timestampTo', timestampTo],
        ['format', format],
      ];

      optionalParams.forEach(([key, value]) => {
        if (value) requestParams.set(key, value);
      });

      const url = `${BASE_PARAMS.basePath}/api/clusters/${encodeURIComponent(
        clusterName
      )}/topics/${encodeURIComponent(
        topicName
      )}/messages/download?${requestParams.toString()}`;

      const response = await fetch(url, {
        method: 'GET',
        credentials: BASE_PARAMS.credentials as RequestCredentials,
      });

      if (!response.ok) {
        await showServerError(response);
        throw new Error('Failed to download messages ZIP');
      }

      const blob = await response.blob();
      const downloadUrl = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      const fallbackFileName = `${topicName}-last-${limit}-messages.zip`;

      anchor.href = downloadUrl;
      anchor.download =
        zipFileNameFromHeader(response.headers.get('content-disposition')) ||
        fallbackFileName;

      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      URL.revokeObjectURL(downloadUrl);
    },
  });
}

export function useUploadMessages() {
  return useMutation({
    mutationFn: async ({
      clusterName,
      topicName,
      files,
      parseMode,
      partitionStrategy,
      keyMode,
      partition,
      partitions,
      keySerde,
      valueSerde,
      headersJson,
      includeMetadataHeaders,
      dryRun,
      messageLimit,
    }: UploadMessagesProps): Promise<UploadMessagesResult> => {
      const formData = new FormData();

      files.forEach((file) => formData.append('files', file));
      formData.set('parseMode', parseMode);
      formData.set('partitionStrategy', partitionStrategy);
      formData.set('keyMode', keyMode);
      formData.set('includeMetadataHeaders', includeMetadataHeaders.toString());
      formData.set('dryRun', dryRun.toString());

      if (partition) formData.set('partition', partition);
      if (keySerde) formData.set('keySerde', keySerde);
      if (valueSerde) formData.set('valueSerde', valueSerde);
      if (headersJson) formData.set('headersJson', headersJson);
      if (messageLimit) formData.set('messageLimit', messageLimit);
      partitions?.forEach((item) => formData.append('partitions', item.toString()));

      const response = await fetch(
        `${BASE_PARAMS.basePath}/api/clusters/${encodeURIComponent(
          clusterName
        )}/topics/${encodeURIComponent(topicName)}/messages/upload`,
        {
          method: 'POST',
          credentials: BASE_PARAMS.credentials as RequestCredentials,
          body: formData,
        }
      );

      if (!response.ok) {
        await showServerError(response);
        throw new Error('Failed to upload messages');
      }

      return response.json();
    },
  });
}

export function useRegisterSmartFilter({
  clusterName,
  topicName,
}: {
  clusterName: ClusterName;
  topicName: TopicName;
}) {
  return useMutation({
    mutationFn: (payload: { filterCode: string }) => {
      return messagesApiClient.registerFilter({
        clusterName,
        topicName,
        messageFilterRegistration: { filterCode: payload.filterCode },
      });
    },
  });
}
