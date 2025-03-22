import React from 'react';
import EditorViewer from 'components/common/EditorViewer/EditorViewer';
import BytesFormatted from 'components/common/BytesFormatted/BytesFormatted';
import ClipboardIcon from 'components/common/Icons/ClipboardIcon';
import { Button } from 'components/common/Button/Button';
import Flexbox from 'components/common/FlexBox/FlexBox';
import { SchemaType, TopicMessageTimestampTypeEnum } from 'generated-sources';
import { formatTimestamp } from 'lib/dateTimeHelpers';
import useDataSaver from 'lib/hooks/useDataSaver';

import * as S from './MessageContent.styled';

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
  valueSerde,
}) => {
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

  const tabContent = activeTabContent() || '';

  const { copyToClipboard } = useDataSaver('topic-message', tabContent);

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
    messageContent && messageContent.trim().startsWith('{')
      ? SchemaType.JSON
      : SchemaType.PROTOBUF;

  return (
    <S.Wrapper>
      <td colSpan={10}>
        <S.Section>
          <S.ContentBox>
            <S.Tabs>
              <Flexbox justifyContent="space-between">
                <Flexbox>
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
                </Flexbox>
                <Flexbox>
                  <Button
                    type="button"
                    buttonSize="M"
                    buttonType="text"
                    onClick={copyToClipboard}
                  >
                    <ClipboardIcon />
                  </Button>
                </Flexbox>
              </Flexbox>
            </S.Tabs>
            <EditorViewer
              data={tabContent}
              maxLines={28}
              schemaType={contentType}
            />
          </S.ContentBox>
          <S.MetadataWrapper>
            <S.Metadata>
              <S.MetadataLabel>Timestamp</S.MetadataLabel>
              <span>
                <S.MetadataValue>{formatTimestamp(timestamp)}</S.MetadataValue>
                <S.MetadataMeta>Timestamp type: {timestampType}</S.MetadataMeta>
              </span>
            </S.Metadata>

            <S.Metadata>
              <S.MetadataLabel>Key Serde</S.MetadataLabel>
              <span>
                <S.MetadataValue>{keySerde}</S.MetadataValue>
                <S.MetadataMeta>
                  Size: <BytesFormatted value={keySize} />
                </S.MetadataMeta>
              </span>
            </S.Metadata>

            <S.Metadata>
              <S.MetadataLabel>Value Serde</S.MetadataLabel>
              <span>
                <S.MetadataValue>{valueSerde}</S.MetadataValue>
                <S.MetadataMeta>
                  Size: <BytesFormatted value={contentSize} />
                </S.MetadataMeta>
              </span>
            </S.Metadata>
          </S.MetadataWrapper>
        </S.Section>
      </td>
    </S.Wrapper>
  );
};

export default MessageContent;
