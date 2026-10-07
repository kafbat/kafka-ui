import React from 'react';
import useDataSaver from 'lib/hooks/useDataSaver';
import { Action, ResourceType, TopicMessage } from 'generated-sources';
import MessageToggleIcon from 'components/common/Icons/MessageToggleIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import { Dropdown, DropdownItem } from 'components/common/Dropdown';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import { formatTimestamp, timeAgo } from 'lib/dateTimeHelpers';
import { JSONPath } from 'jsonpath-plus';
import Ellipsis from 'components/common/Ellipsis/Ellipsis';
import WarningRedIcon from 'components/common/Icons/WarningRedIcon';
import Tooltip from 'components/common/Tooltip/Tooltip';
import { useTimezone } from 'lib/hooks/useTimezones';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import { useTopicActions } from 'components/contexts/TopicActionsContext';
import ClusterContext from 'components/contexts/ClusterContext';
import { useConfirm } from 'lib/hooks/useConfirm';

import {
  CONTENT_PREVIEW_LENGTH,
  previewHeaders,
  truncatePreview,
} from './preview';
import MessageContent from './MessageContent/MessageContent';
import * as S from './MessageContent/MessageContent.styled';

export interface PreviewFilter {
  field: string;
  path: string;
}

export interface Props {
  keyFilters: PreviewFilter[];
  contentFilters: PreviewFilter[];
  message: TopicMessage;
}

const Message: React.FC<Props> = ({ message, keyFilters, contentFilters }) => {
  const { currentTimezone } = useTimezone();
  const { topicName } = useAppParams<RouteParamsClusterTopic>();
  const { openSidebarWithMessage } = useTopicActions();
  const confirm = useConfirm();
  const [isOpen, setIsOpen] = React.useState(false);
  const { messageRelativeTimestamp } = React.useContext(ClusterContext);

  const {
    timestamp,
    timestampType,
    offset,
    key,
    keySize,
    partition,
    value,
    valueSize,
    headers,
    valueSerde,
    keySerde,
    valueDeserializeProperties,
    keyDeserializeProperties,
  } = message;

  const createSavedMessage = () =>
    JSON.stringify(
      {
        Value: value,
        Offset: offset,
        Key: key,
        Partition: partition,
        Headers: headers,
        Timestamp: timestamp,
      },
      null,
      '\t'
    );
  const { copyToClipboard, saveFile } = useDataSaver(
    'topic-message',
    createSavedMessage
  );
  const isLargeMessage = React.useMemo(
    () =>
      (key?.length || 0) > CONTENT_PREVIEW_LENGTH ||
      (value?.length || 0) > CONTENT_PREVIEW_LENGTH ||
      previewHeaders(headers).truncated,
    [key, value, headers]
  );

  const toggleIsOpen = () => setIsOpen(!isOpen);

  const [vEllipsisOpen, setVEllipsisOpen] = React.useState(false);

  const getParsedJson = (jsonValue: string) => {
    try {
      return JSON.parse(jsonValue);
    } catch {
      return {};
    }
  };

  const renderFilteredJson = (
    jsonValue?: string,
    filters?: PreviewFilter[]
  ) => {
    if (!filters?.length || !jsonValue) return truncatePreview(jsonValue);
    if (jsonValue.length > CONTENT_PREVIEW_LENGTH) {
      return (
        <>
          {truncatePreview(jsonValue)}
          <span> (JSONPath preview skipped for large content)</span>
        </>
      );
    }
    const parsedJson = getParsedJson(jsonValue);

    return (
      <>
        {filters.map((item) => {
          return (
            <div key={`${item.path}--${item.field}`}>
              {item.field}:{' '}
              {truncatePreview(
                JSON.stringify(
                  JSONPath({ path: item.path, json: parsedJson, wrap: false })
                )
              )}
            </div>
          );
        })}
      </>
    );
  };

  const messageTimestamp = formatTimestamp({
    timestamp,
    timezone: currentTimezone.value,
    withMilliseconds: true,
  });

  return (
    <>
      <S.ClickableRow
        $truncated={isLargeMessage}
        onMouseEnter={() => setVEllipsisOpen(true)}
        onMouseLeave={() => setVEllipsisOpen(false)}
        onClick={toggleIsOpen}
      >
        <td>
          <IconButtonWrapper aria-hidden>
            <MessageToggleIcon isOpen={isOpen} />
          </IconButtonWrapper>
        </td>
        <td>{offset}</td>
        <td>{partition}</td>
        <td>
          {messageRelativeTimestamp ? (
            <Tooltip value={timeAgo(timestamp)} content={messageTimestamp} />
          ) : (
            <div>{messageTimestamp}</div>
          )}
        </td>
        <S.DataCell title={truncatePreview(key)}>
          <Ellipsis text={renderFilteredJson(key, keyFilters)}>
            {keySerde === 'Fallback' && (
              <Tooltip
                value={<WarningRedIcon />}
                content="Fallback serde was used"
                placement="left"
              />
            )}
          </Ellipsis>
        </S.DataCell>
        <S.DataCell title={truncatePreview(value)}>
          {isLargeMessage && (
            <S.RowSummary>
              <S.RowTitle>Preview truncated</S.RowTitle>
              <S.RowDescription>
                Expand to inspect; download for full content.
              </S.RowDescription>
            </S.RowSummary>
          )}
          <S.Metadata>
            <S.MetadataValue $truncated={isLargeMessage}>
              <Ellipsis text={renderFilteredJson(value, contentFilters)}>
                {valueSerde === 'Fallback' && (
                  <Tooltip
                    value={<WarningRedIcon />}
                    content="Fallback serde was used"
                    placement="left"
                  />
                )}
              </Ellipsis>
            </S.MetadataValue>
          </S.Metadata>
        </S.DataCell>
        <td style={{ width: '5%' }}>
          <div style={{ visibility: vEllipsisOpen ? 'visible' : 'hidden' }}>
            <Dropdown>
              <DropdownItem
                aria-label="Copy to clipboard"
                onClick={copyToClipboard}
              >
                Copy to clipboard
              </DropdownItem>
              <DropdownItem aria-label="Save as a file" onClick={saveFile}>
                Save as a file
              </DropdownItem>
              <ActionDropdownItem
                aria-label="Reproduce message"
                onClick={() => {
                  if (isLargeMessage) {
                    confirm(
                      'Opening this large message in the producer editor may make this tab slow or unresponsive. Download the message instead to inspect it without opening the editor.',
                      () => openSidebarWithMessage(message),
                      {
                        title: 'Open large message in producer editor?',
                        confirmLabel: 'Open anyway',
                      }
                    );
                  } else {
                    openSidebarWithMessage(message);
                  }
                }}
                permission={{
                  resource: ResourceType.TOPIC,
                  action: Action.MESSAGES_PRODUCE,
                  value: topicName,
                }}
              >
                Reproduce message
              </ActionDropdownItem>
            </Dropdown>
          </div>
        </td>
      </S.ClickableRow>
      {isOpen && (
        <MessageContent
          key={`${partition}-${offset}`}
          messageKey={key}
          messageContent={value}
          headers={headers}
          timestamp={timestamp}
          timestampType={timestampType}
          keySize={keySize}
          contentSize={valueSize}
          keySerde={keySerde}
          valueSerde={valueSerde}
          valueDeserializeProperties={valueDeserializeProperties}
          keyDeserializeProperties={keyDeserializeProperties}
        />
      )}
    </>
  );
};

export default Message;
