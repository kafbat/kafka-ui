import PageLoader from 'components/common/PageLoader/PageLoader';
import { Table } from 'components/common/table/Table/Table.styled';
import TableHeaderCell from 'components/common/table/TableHeaderCell/TableHeaderCell';
import { TopicMessage } from 'generated-sources';
import { format } from 'date-fns';
import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { Button } from 'components/common/Button/Button';
import * as S from 'components/common/NewTable/Table.styled';
import { usePaginateTopics, useIsLiveMode } from 'lib/hooks/useMessagesFilters';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';
import useDataSaver from 'lib/hooks/useDataSaver';
import Select, { SelectOption } from 'components/common/Select/Select';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';

import Message, { PreviewFilter } from './Message';
import PreviewModal from './PreviewModal';

export interface MessagesTableProps {
  messages: TopicMessage[];
  isFetching: boolean;
}

interface MessageData {
  Value: string | undefined;
  Offset: number;
  Key: string | undefined;
  Partition: number;
  Headers: { [key: string]: string | undefined } | undefined;
  Timestamp: Date;
}

type DownloadFormat = 'json' | 'csv';

function padCurrentDateTimeString(): string {
  const now: Date = new Date();
  const dateTimeString: string = format(now, 'yyyy-MM-dd HH:mm:ss');

  return `_${dateTimeString}`;

  interface MessagePreviewProps {
  [key: string]: {
    keyFilters: PreviewFilter[];
    contentFilters: PreviewFilter[];
  };
}

const MessagesTable: React.FC<MessagesTableProps> = ({
  messages,
  isFetching,
}) => {
  const paginate = usePaginateTopics();
  const [previewFor, setPreviewFor] = useState<'key' | 'content' | null>(null);
  const [keyFilters, setKeyFilters] = useState<PreviewFilter[]>([]);
  const [contentFilters, setContentFilters] = useState<PreviewFilter[]>([]);
  const nextCursor = useMessageFiltersStore((state) => state.nextCursor);
  const isLive = useIsLiveMode();
  const { topicName } = useAppParams<RouteParamsClusterTopic>();
  const [messagesPreview, setMessagesPreview] =
    useLocalStorage<MessagePreviewProps>('message-preview', {
      [topicName]: {
        keyFilters: [],
        contentFilters: [],
      },
    });

  useEffect(() => {
    setKeyFilters(messagesPreview[topicName]?.keyFilters || []);
    setContentFilters(messagesPreview[topicName]?.contentFilters || []);
  }, []);

  const setFilters = useCallback(
    (payload: PreviewFilter[]) => {
      if (previewFor === 'key') {
        setKeyFilters(payload);
        setMessagesPreview({
          ...messagesPreview,
          [topicName]: {
            ...messagesPreview[topicName],
            keyFilters: payload,
          },
        });
      } else {
        setContentFilters(payload);
        setMessagesPreview({
          ...messagesPreview,
          [topicName]: {
            ...messagesPreview[topicName],
            contentFilters: payload,
          },
        });
      }
    },
    [previewFor, messagesPreview, topicName]
  );

  const [selectedFormat, setSelectedFormat] = useState<DownloadFormat>('json');

  const formatOptions: SelectOption<DownloadFormat>[] = [
    { label: 'JSON', value: 'json' },
    { label: 'CSV', value: 'csv' },
  ];

  const baseFileName = `topic-messages${padCurrentDateTimeString()}`;

  const savedMessagesJson: MessageData[] = messages.map((message) => ({
    Value: message.content,
    Offset: message.offset,
    Key: message.key,
    Partition: message.partition,
    Headers: message.headers,
    Timestamp: message.timestamp,
  }));

  const convertToCSV = useMemo(() => {
    return (messagesData: MessageData[]) => {
      const headers = [
        'Value',
        'Offset',
        'Key',
        'Partition',
        'Headers',
        'Timestamp',
      ] as const;
      const rows = messagesData.map((msg) =>
        headers
          .map((header) => {
            const value = msg[header];
            if (header === 'Headers') {
              return JSON.stringify(value || {});
            }
            return String(value ?? '');
          })
          .join(',')
      );
      return [headers.join(','), ...rows].join('\n');
    };
  }, []);

  const jsonSaver = useDataSaver(
    `${baseFileName}.json`,
    JSON.stringify(savedMessagesJson, null, '\t')
  );
  const csvSaver = useDataSaver(
    `${baseFileName}.csv`,
    convertToCSV(savedMessagesJson)
  );

  const handleFormatSelect = (downloadFormat: DownloadFormat) => {
    setSelectedFormat(downloadFormat);
  };

  const handleDownload = () => {
    if (selectedFormat === 'json') {
      jsonSaver.saveFile();
    } else {
      csvSaver.saveFile();
    }
  };

  return (
    <div style={{ position: 'relative' }}>
      <div
        style={{
          display: 'flex',
          gap: '8px',
          marginLeft: '1rem',
          marginBottom: '1rem',
        }}
      >
        <Select<DownloadFormat>
          id="download-format"
          name="download-format"
          onChange={handleFormatSelect}
          options={formatOptions}
          value={selectedFormat}
          minWidth="70px"
          selectSize="M"
          placeholder="Select format to download"
          disabled={isFetching || messages.length === 0}
        />
        <Button
          disabled={isFetching || messages.length === 0}
          buttonType="secondary"
          buttonSize="M"
          onClick={handleDownload}
        >
          Download Current Messages
        </Button>
      </div>

      {previewFor !== null && (
        <PreviewModal
          values={previewFor === 'key' ? keyFilters : contentFilters}
          toggleIsOpen={() => setPreviewFor(null)}
          setFilters={setFilters}
        />
      )}
      <Table isFullwidth>
        <thead>
          <tr>
            <TableHeaderCell> </TableHeaderCell>
            <TableHeaderCell title="Offset" />
            <TableHeaderCell title="Partition" />
            <TableHeaderCell title="Timestamp" />
            <TableHeaderCell
              title="Key"
              previewText={`Preview ${
                keyFilters.length ? `(${keyFilters.length} selected)` : ''
              }`}
              onPreview={() => setPreviewFor('key')}
            />
            <TableHeaderCell
              title="Value"
              previewText={`Preview ${
                contentFilters.length
                  ? `(${contentFilters.length} selected)`
                  : ''
              }`}
              onPreview={() => setPreviewFor('content')}
            />
            <TableHeaderCell> </TableHeaderCell>
          </tr>
        </thead>
        <tbody>
          {messages.map((message: TopicMessage) => (
            <Message
              key={[
                message.offset,
                message.timestamp,
                message.key,
                message.partition,
              ].join('-')}
              message={message}
              keyFilters={keyFilters}
              contentFilters={contentFilters}
            />
          ))}
          {isFetching && !messages.length && (
            <tr>
              <td colSpan={10}>
                <PageLoader />
              </td>
            </tr>
          )}
          {messages.length === 0 && !isFetching && (
            <tr>
              <td colSpan={10}>No messages found</td>
            </tr>
          )}
        </tbody>
      </Table>
      <S.Pagination>
        <S.Pages>
          <Button
            disabled={isLive || isFetching || !nextCursor}
            buttonType="secondary"
            buttonSize="L"
            onClick={paginate}
          >
            Next →
          </Button>
        </S.Pages>
      </S.Pagination>
    </div>
  );
};

export default MessagesTable;
