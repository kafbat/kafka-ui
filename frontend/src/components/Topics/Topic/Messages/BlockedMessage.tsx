import React from 'react';
import { TopicMessage, TopicMessageBlocked } from 'generated-sources';
import { Button } from 'components/common/Button/Button';
import { useConfirm } from 'lib/hooks/useConfirm';
import { apiFetch, showAlert } from 'lib/errorHandling';
import { messagesApiClient } from 'lib/api';
import { downloadTopicMessage } from 'lib/hooks/api/topicMessages';
import { formatBytes } from 'components/common/BytesFormatted/utils';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import { useTimezone } from 'lib/hooks/useTimezones';

import * as S from './BlockedMessage.styled';

interface Props {
  blockedMessage: TopicMessageBlocked;
  clusterName: string;
  topicName: string;
  fetchRequestId: number;
  keySerde?: string;
  valueSerde?: string;
  onOpen: (message: TopicMessage, requestId: number) => void;
}

/** Renders an oversized record placeholder with explicit open and download actions. */
const BlockedMessage: React.FC<Props> = ({
  blockedMessage,
  clusterName,
  topicName,
  fetchRequestId,
  keySerde,
  valueSerde,
  onOpen,
}) => {
  const confirm = useConfirm();
  const { currentTimezone } = useTimezone();
  const [isDownloading, setIsDownloading] = React.useState(false);
  const { partition, offset, size, timestamp } = blockedMessage;
  const messageParams = {
    clusterName,
    topicName,
    partition,
    offset,
    keySerde,
    valueSerde,
  };

  /** Confirms the browser risk before fetching and displaying the full record. */
  const openMessage = () => {
    confirm(
      <div>
        Rendering this {formatBytes(size)} message may make the current browser
        tab slow or unresponsive. Other tabs and Kafka are not affected.
      </div>,
      async () => {
        const requestId = fetchRequestId;
        try {
          const message = await apiFetch(() =>
            messagesApiClient.downloadTopicMessage(messageParams)
          );
          onOpen(message, requestId);
        } catch (error) {
          showAlert('error', {
            title: 'Could not open message',
            message:
              typeof error === 'object' && error && 'message' in error
                ? String(error.message)
                : 'An error occurred while loading the message.',
          });
        }
      },
      {
        title: 'Open large message?',
        confirmLabel: 'Open anyway',
      }
    );
  };

  /** Downloads the full record without adding it to the rendered message table. */
  const downloadMessage = async () => {
    setIsDownloading(true);
    try {
      await downloadTopicMessage(messageParams);
    } catch (error) {
      showAlert('error', {
        title: 'Could not download message',
        message:
          error instanceof Response
            ? `${error.status} ${error.statusText}`
            : 'An error occurred while downloading the message.',
      });
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <S.Row>
      <td aria-hidden />
      <td>{offset}</td>
      <td>{partition}</td>
      <td>
        {timestamp
          ? formatTimestamp({
              timestamp,
              timezone: currentTimezone.value,
              withMilliseconds: true,
            })
          : 'Unavailable'}
      </td>
      <td colSpan={2}>
        <S.Summary>
          <S.Title>Message blocked to protect this tab</S.Title>
          <S.Description>
            {formatBytes(size)} exceeds the browser rendering limit. Inspect it
            only if this tab can safely become unresponsive.
          </S.Description>
        </S.Summary>
      </td>
      <td>
        <S.Actions>
          <Button buttonType="secondary" buttonSize="M" onClick={openMessage}>
            Open anyway
          </Button>
          <Button
            buttonType="text"
            buttonSize="M"
            onClick={downloadMessage}
            inProgress={isDownloading}
          >
            Download message
          </Button>
        </S.Actions>
      </td>
    </S.Row>
  );
};

export default BlockedMessage;
