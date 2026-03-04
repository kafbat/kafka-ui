import React from 'react';
import EditorViewer from 'components/common/EditorViewer/EditorViewer';
import {
  SchemaType,
  TopicMessage,
  TopicMessageTimestampTypeEnum,
} from 'generated-sources';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import { useTimezone } from 'lib/hooks/useTimezones';

import * as S from './MessageContent.styled';
import Serde from './components/Serde/Serde';

type Tab = 'key' | 'content' | 'headers';

export interface MessageContentProps {
  messageKey?: string;
  messageContent?: string;
  headers?: { [key: string]: string | undefined };
  timestamp?: Date;
  timestampType?: TopicMessageTimestampTypeEnum;
  keySize?: number;
  contentSize?: number;
  keySerde?: string;
  valueSerde?: string;
  valueDeserializeProperties?: TopicMessage['valueDeserializeProperties'];
  keyDeserializeProperties?: TopicMessage['keyDeserializeProperties'];
}

const MessageContent: React.FC<MessageContentProps> = ({
  messageKey,
  messageContent,
  headers,
  timestamp,
  timestampType,
  keySize,
  contentSize,
  keySerde,
  keyDeserializeProperties,
  valueDeserializeProperties,
  valueSerde,
}) => {
  const { currentTimezone } = useTimezone();

  const [activeTab, setActiveTab] = React.useState<Tab>('content');
  const activeTabContent = () => {
    switch (activeTab) {
      case 'content':
        return messageContent;
      case 'key':
        return messageKey;
      default:
        return JSON.stringify(headers);
    }
  };

  const handleKeyTabClick = (e: React.MouseEvent) => {
    e.preventDefault();
    setActiveTab('key');
  };

  const handleContentTabClick = (e: React.MouseEvent) => {
    e.preventDefault();
    setActiveTab('content');
  };

  const handleHeadersTabClick = (e: React.MouseEvent) => {
    e.preventDefault();
    setActiveTab('headers');
  };

  const contentType =
    messageContent &&
    (messageContent.trim().startsWith('{') ||
      messageContent.trim().startsWith('['))
      ? SchemaType.JSON
      : SchemaType.PROTOBUF;

  return (
    <S.Wrapper>
      <td colSpan={10}>
        <S.Section>
          <S.ContentBox>
            <S.Tabs>
              <S.Tab
                type="button"
                $active={activeTab === 'key'}
                onClick={handleKeyTabClick}
              >
                Key
              </S.Tab>
              <S.Tab
                $active={activeTab === 'content'}
                type="button"
                onClick={handleContentTabClick}
              >
                Value
              </S.Tab>
              <S.Tab
                $active={activeTab === 'headers'}
                type="button"
                onClick={handleHeadersTabClick}
              >
                Headers
              </S.Tab>
            </S.Tabs>
            <EditorViewer
              data={activeTabContent() || ''}
              maxLines={28}
              schemaType={contentType}
            />
          </S.ContentBox>
          <S.MetadataWrapper>
            <S.Metadata>
              <S.MetadataLabel>Timestamp</S.MetadataLabel>
              <span>
                <S.MetadataValue>
                  {formatTimestamp({
                    timestamp,
                    timezone: currentTimezone.value,
                    withMilliseconds: true,
                  })}
                </S.MetadataValue>
                <S.MetadataMeta>Timestamp type: {timestampType}</S.MetadataMeta>
              </span>
            </S.Metadata>
            <Serde
              title="Key Serde"
              serde={keySerde}
              size={keySize}
              properties={keyDeserializeProperties}
            />
            <Serde
              title="Value Serde"
              serde={valueSerde}
              size={contentSize}
              properties={valueDeserializeProperties}
            />
          </S.MetadataWrapper>
        </S.Section>
      </td>
    </S.Wrapper>
  );
};

export default MessageContent;
