import { TextEncoder } from 'util';

import React from 'react';
import { screen, within } from '@testing-library/react';
import MessageContent, {
  MessageContentProps,
} from 'components/Topics/Topic/Messages/MessageContent/MessageContent';
import { TopicMessageTimestampTypeEnum } from 'generated-sources';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import { theme } from 'theme/theme';
import { CONTENT_PREVIEW_LENGTH } from 'components/Topics/Topic/Messages/preview';
import Message from 'components/Topics/Topic/Messages/Message';
import { TopicActionsProvider } from 'components/contexts/TopicActionsContext';
import useAppParams from 'lib/hooks/useAppParams';

jest.mock('lib/hooks/useAppParams');

const setupWrapper = (props?: Partial<MessageContentProps>) => {
  return (
    <table>
      <tbody>
        <MessageContent
          downloadFilenamePrefix="orders.created-p2-o42"
          onDownload={jest.fn()}
          messageKey='"test-key"'
          messageContent='{"data": "test"}'
          headers={{ header: 'test' }}
          timestamp={new Date(0)}
          timestampType={TopicMessageTimestampTypeEnum.CREATE_TIME}
          keySerde="SchemaRegistry"
          valueSerde="Avro"
          {...props}
        />
      </tbody>
    </table>
  );
};

global.TextEncoder = TextEncoder as typeof global.TextEncoder;

describe('MessageContent screen', () => {
  beforeEach(() => {
    jest.mocked(useAppParams).mockReturnValue({
      clusterName: 'test-cluster',
      topicName: 'orders.created',
    });
  });

  describe('large message content', () => {
    const value = `${'v'.repeat(CONTENT_PREVIEW_LENGTH * 2)}value end`;
    const key = `${'k'.repeat(CONTENT_PREVIEW_LENGTH + 1)}key end`;
    const headers = { large: 'h'.repeat(CONTENT_PREVIEW_LENGTH * 2) };

    it('resets bounded viewing when switching tabs', async () => {
      render(setupWrapper({ messageContent: value, messageKey: key, headers }));
      await userEvent.click(screen.getByText('Show next chunk'));
      await userEvent.click(screen.getAllByText('Key')[0]);
      expect(screen.getByText('Previous chunk')).toBeDisabled();
      await userEvent.click(screen.getAllByText('Value')[0]);
      expect(screen.getByText('Previous chunk')).toBeDisabled();
      await userEvent.click(screen.getByText('Headers'));
      expect(screen.getByRole('status')).toHaveTextContent(
        'Headers exceed the preview limit'
      );
      expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('resets viewing for a different record with identical content', async () => {
      const record = (offset: number) => (
        <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
          <table>
            <tbody>
              <Message
                message={{
                  partition: 0,
                  offset,
                  timestamp: new Date(0),
                  value,
                }}
                keyFilters={[]}
                contentFilters={[]}
              />
            </tbody>
          </table>
        </TopicActionsProvider>
      );
      const { rerender } = render(record(1));
      await userEvent.click(screen.getByRole('row'));
      await userEvent.click(
        screen.getByRole('button', { name: 'Show next chunk' })
      );
      expect(
        screen.getByRole('button', { name: 'Previous chunk' })
      ).toBeEnabled();
      rerender(record(2));
      expect(
        screen.getByRole('button', { name: 'Previous chunk' })
      ).toBeDisabled();
      expect(screen.getByRole('status')).toHaveTextContent(
        `Chunk 1 of 3 visible. Preview range: 1–${CONTENT_PREVIEW_LENGTH.toLocaleString(
          'en-US'
        )} of`
      );
    });

    it.each([
      ['large', value, key, headers],
      ['small', '{"order":42}', 'order-key', { source: 'test' }],
    ])(
      'downloads the full message as JSON from every field for %s content',
      async (_size, messageContent, messageKey, messageHeaders) => {
        const blobs: Blob[] = [];
        const filenames: string[] = [];
        global.URL.createObjectURL = jest.fn((blob: Blob) => {
          blobs.push(blob);
          return 'blob:message-content';
        });
        global.URL.revokeObjectURL = jest.fn();
        const click = jest
          .spyOn(HTMLAnchorElement.prototype, 'click')
          .mockImplementation(function captureFilename(
            this: HTMLAnchorElement
          ) {
            filenames.push(this.download);
          });
        render(
          <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
            <table>
              <tbody>
                <Message
                  message={{
                    partition: 2,
                    offset: 42,
                    timestamp: new Date(0),
                    value: messageContent,
                    key: messageKey,
                    headers: messageHeaders,
                  }}
                  keyFilters={[]}
                  contentFilters={[]}
                />
              </tbody>
            </table>
          </TopicActionsProvider>
        );
        await userEvent.click(screen.getByRole('row'));
        await userEvent.click(screen.getByText('Download full message'));
        await userEvent.click(
          screen.getByRole('button', { name: 'Download value' })
        );
        await userEvent.click(screen.getAllByText('Key')[0]);
        await userEvent.click(screen.getByText('Download full message'));
        await userEvent.click(
          screen.getByRole('button', { name: 'Download key' })
        );
        await userEvent.click(screen.getByText('Headers'));
        await userEvent.click(screen.getByText('Download full message'));
        await userEvent.click(
          screen.getByRole('button', { name: 'Download headers' })
        );
        const contents = await Promise.all(
          blobs.map(
            (blob) =>
              new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result);
                reader.onerror = reject;
                reader.readAsText(blob);
              })
          )
        );
        const expectedMessage = {
          Value: messageContent,
          Offset: 42,
          Key: messageKey,
          Partition: 2,
          Headers: messageHeaders,
          Timestamp: new Date(0).toISOString(),
        };
        expect(contents).toEqual([
          JSON.stringify(expectedMessage, null, '\t'),
          messageContent,
          JSON.stringify(expectedMessage, null, '\t'),
          messageKey,
          JSON.stringify(expectedMessage, null, '\t'),
          JSON.stringify(messageHeaders),
        ]);
        expect(filenames).toEqual([
          'orders.created-p2-o42.json',
          'orders.created-p2-o42-value',
          'orders.created-p2-o42.json',
          'orders.created-p2-o42-key',
          'orders.created-p2-o42.json',
          'orders.created-p2-o42-headers',
        ]);
        expect(global.URL.revokeObjectURL).toHaveBeenCalledTimes(6);
        click.mockRestore();
      }
    );
  });

  describe('small message content', () => {
    beforeEach(() => {
      render(setupWrapper());
    });

    it('separates download actions from the field selector', () => {
      const actions = screen.getByRole('group', { name: 'Message actions' });
      const fields = screen.getByRole('group', {
        name: 'Message content fields',
      });
      expect(actions).toHaveStyleRule('margin-left', '12px');
      expect(
        within(fields).getByRole('button', { name: 'Value' })
      ).toHaveAttribute('aria-pressed', 'true');
      const copyButton = within(actions).getByRole('button', {
        name: 'Copy value to clipboard',
      });
      expect(copyButton).toHaveTextContent('Copy');
      expect(copyButton.querySelector('svg')).toHaveStyleRule('width', '16px');
      expect(copyButton.querySelector('svg')).toHaveStyleRule('height', '16px');
      expect(
        within(actions).getByRole('button', { name: 'Download full message' })
      ).toBeInTheDocument();
    });

    describe('Checking keySerde and valueSerde', () => {
      it('keySerde in document', () => {
        expect(screen.getByText('SchemaRegistry')).toBeInTheDocument();
      });

      it('valueSerde in document', () => {
        expect(screen.getByText('Avro')).toBeInTheDocument();
      });
    });

    describe('when switched to display the key', () => {
      it('makes key tab active', async () => {
        const keyTab = screen.getAllByText('Key');
        await userEvent.click(keyTab[0]);
        expect(keyTab[0]).toHaveStyleRule(
          'background-color',
          theme.secondaryTab.backgroundColor.active
        );
        expect(keyTab[0]).toHaveAttribute('aria-pressed', 'true');
        expect(
          screen.getByRole('button', { name: 'Copy key to clipboard' })
        ).toBeInTheDocument();
      });
    });

    describe('when switched to display the headers', () => {
      it('makes Headers tab active', async () => {
        await userEvent.click(screen.getByText('Headers'));
        expect(screen.getByText('Headers')).toHaveStyleRule(
          'background-color',
          theme.secondaryTab.backgroundColor.active
        );
        expect(screen.getByText('Headers')).toHaveAttribute(
          'aria-pressed',
          'true'
        );
        expect(
          screen.getByRole('button', { name: 'Copy headers to clipboard' })
        ).toBeInTheDocument();
      });
    });

    describe('when switched to display the value', () => {
      it('makes value tab active', async () => {
        const contentTab = screen.getAllByText('Value');
        await userEvent.click(contentTab[0]);
        expect(contentTab[0]).toHaveStyleRule(
          'background-color',
          theme.secondaryTab.backgroundColor.active
        );
        expect(contentTab[0]).toHaveAttribute('aria-pressed', 'true');
      });
    });
  });
});
