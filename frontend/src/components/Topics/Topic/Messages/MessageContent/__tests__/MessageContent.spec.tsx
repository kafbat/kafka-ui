import { TextEncoder } from 'util';

import React from 'react';
import { screen } from '@testing-library/react';
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

const setupWrapper = (props?: Partial<MessageContentProps>) => {
  return (
    <table>
      <tbody>
        <MessageContent
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
  describe('large message content', () => {
    const value = `${'v'.repeat(CONTENT_PREVIEW_LENGTH * 2)}value end`;
    const key = `${'k'.repeat(CONTENT_PREVIEW_LENGTH + 1)}key end`;
    const headers = { large: 'h'.repeat(CONTENT_PREVIEW_LENGTH * 2) };

    it('resets bounded viewing when switching tabs', async () => {
      render(setupWrapper({ messageContent: value, messageKey: key, headers }));
      await userEvent.click(screen.getByText('Show more'));
      await userEvent.click(screen.getAllByText('Key')[0]);
      expect(screen.getByText('Previous preview')).toBeDisabled();
      await userEvent.click(screen.getAllByText('Value')[0]);
      expect(screen.getByText('Previous preview')).toBeDisabled();
      await userEvent.click(screen.getByText('Headers'));
      expect(screen.getByRole('status')).toHaveTextContent(
        'Headers preview truncated'
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
      await userEvent.click(screen.getByRole('button', { name: 'Show more' }));
      expect(
        screen.getByRole('button', { name: 'Previous preview' })
      ).toBeEnabled();
      rerender(record(2));
      expect(
        screen.getByRole('button', { name: 'Previous preview' })
      ).toBeDisabled();
      expect(screen.getByRole('status')).toHaveTextContent(
        `Showing characters 1-${CONTENT_PREVIEW_LENGTH}`
      );
    });

    it('downloads the complete active field rather than its preview', async () => {
      const blobs: Blob[] = [];
      global.URL.createObjectURL = jest.fn((blob: Blob) => {
        blobs.push(blob);
        return 'blob:message-content';
      });
      global.URL.revokeObjectURL = jest.fn();
      const click = jest
        .spyOn(HTMLAnchorElement.prototype, 'click')
        .mockImplementation();
      render(setupWrapper({ messageContent: value, messageKey: key, headers }));
      await userEvent.click(screen.getByText('Download full content'));
      await userEvent.click(screen.getAllByText('Key')[0]);
      await userEvent.click(screen.getByText('Download full content'));
      await userEvent.click(screen.getByText('Headers'));
      await userEvent.click(screen.getByText('Download full content'));
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
      expect(contents).toEqual([value, key, JSON.stringify(headers)]);
      expect(global.URL.revokeObjectURL).toHaveBeenCalledTimes(3);
      click.mockRestore();
    });
  });

  describe('small message content', () => {
    beforeEach(() => {
      render(setupWrapper());
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
      });
    });

    describe('when switched to display the headers', () => {
      it('makes Headers tab active', async () => {
        await userEvent.click(screen.getByText('Headers'));
        expect(screen.getByText('Headers')).toHaveStyleRule(
          'background-color',
          theme.secondaryTab.backgroundColor.active
        );
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
      });
    });
  });
});
