import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import MessagesTable, {
  MessagesTableProps,
} from 'components/Topics/Topic/Messages/MessagesTable';
import { TopicMessage, TopicMessageTimestampTypeEnum } from 'generated-sources';
import { useIsLiveMode } from 'lib/hooks/useMessagesFilters';
import useAppParams from 'lib/hooks/useAppParams';
import { LOCAL_STORAGE_KEY_PREFIX } from 'lib/constants';
import { TopicActionsProvider } from 'components/contexts/TopicActionsContext';
import { messagesApiClient } from 'lib/api';

export const topicMessagePayload: TopicMessage = {
  partition: 29,
  offset: 14,
  timestamp: new Date('2021-07-21T23:25:14.865Z'),
  timestampType: TopicMessageTimestampTypeEnum.CREATE_TIME,
  key: 'schema-registry',
  headers: {},
  value:
    '{"host":"schemaregistry1","port":8085,"master_eligibility":true,"scheme":"http","version":1}',
};

const mockTopicsMessages = [{ ...topicMessagePayload }];

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('lib/hooks/useMessagesFilters', () => ({
  useIsLiveMode: jest.fn(),
  usePaginateTopics: jest.fn(),
}));

jest.mock('lib/hooks/useAppParams', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('lib/api', () => ({
  messagesApiClient: {
    downloadTopicMessage: jest.fn(),
  },
}));

describe('MessagesTable', () => {
  const renderComponent = (props?: Partial<MessagesTableProps>) => {
    (useAppParams as jest.Mock).mockImplementation(() => ({
      clusterName: 'local',
      topicName: 'testTopic',
    }));
    return render(
      <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
        <MessagesTable messages={[]} isFetching={false} {...props} />
      </TopicActionsProvider>
    );
  };

  describe('Default props Setup for MessagesTable component', () => {
    beforeEach(() => {
      renderComponent();
    });

    it('should check the render', () => {
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('should check preview buttons', async () => {
      const previewButtons = await screen.findAllByRole('button', {
        name: 'Preview',
      });
      expect(previewButtons).toHaveLength(2);
    });

    it('should show preview modal with validation', async () => {
      await userEvent.click(screen.getAllByText('Preview')[0]);
      expect(screen.getByPlaceholderText('Field')).toHaveValue('');
      expect(screen.getByPlaceholderText('Json Path')).toHaveValue('');
    });

    it('should check the if no elements is rendered in the table', () => {
      expect(screen.getByText(/No messages found/i)).toBeInTheDocument();
    });
  });

  describe('Custom Setup with different props value', () => {
    it('should check if next button is disabled isLive Param', () => {
      renderComponent({ isFetching: true });
      expect(screen.queryByText(/next/i)).toBeDisabled();
    });

    it('should check if next button is disabled if there is no nextCursor', () => {
      (useIsLiveMode as jest.Mock).mockImplementation(() => false);
      renderComponent({ isFetching: false });
      expect(screen.queryByText(/next/i)).toBeDisabled();
    });

    it('hides duplicate pagination when the byte limit is reached', () => {
      renderComponent({ bytesLimitReached: true });
      expect(screen.queryByText(/next/i)).not.toBeInTheDocument();
    });

    it('shows a blocked record with explicit recovery actions', () => {
      renderComponent({
        bytesLimitReached: true,
        blockedMessage: { partition: 2, offset: 42, size: 2048 },
      });

      expect(
        screen.getByText('Message blocked to protect this tab')
      ).toBeInTheDocument();
      expect(
        screen.getByText(/2 KB exceeds the browser rendering limit/)
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Open anyway' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Download message' })
      ).toBeInTheDocument();
    });

    it('requires confirmation before opening a blocked record', async () => {
      renderComponent({
        blockedMessage: { partition: 2, offset: 42, size: 2048 },
        keySerde: 'Integer',
        valueSerde: 'Avro',
      });

      await userEvent.click(
        screen.getByRole('button', { name: 'Open anyway' })
      );
      expect(screen.getByText('Open large message?')).toBeInTheDocument();
      expect(
        screen.getAllByRole('button', { name: 'Open anyway' })
      ).toHaveLength(2);
      expect(
        screen.getByText(
          /may make the current browser tab slow or unresponsive/
        )
      ).toBeInTheDocument();
    });

    it('opens a blocked record only after explicit confirmation', async () => {
      (messagesApiClient.downloadTopicMessage as jest.Mock).mockResolvedValue({
        ...topicMessagePayload,
        partition: 2,
        offset: 42,
      });
      renderComponent({
        blockedMessage: { partition: 2, offset: 42, size: 2048 },
        keySerde: 'Integer',
        valueSerde: 'Avro',
      });

      await userEvent.click(
        screen.getByRole('button', { name: 'Open anyway' })
      );
      await userEvent.click(
        screen
          .getByRole('dialog', { name: 'Open large message?' })
          .querySelector('button:last-child') as HTMLButtonElement
      );

      await waitFor(() =>
        expect(
          screen.queryByText('Message blocked to protect this tab')
        ).not.toBeInTheDocument()
      );
      expect(messagesApiClient.downloadTopicMessage).toHaveBeenCalledWith({
        clusterName: 'local',
        topicName: 'testTopic',
        partition: 2,
        offset: 42,
        keySerde: 'Integer',
        valueSerde: 'Avro',
      });
      expect(
        screen.getByText(topicMessagePayload.value || '')
      ).toBeInTheDocument();
    });

    it('discards a blocked-message response when a newer fetch starts', async () => {
      let resolveDownload: (message: TopicMessage) => void = () => {};
      (messagesApiClient.downloadTopicMessage as jest.Mock).mockReturnValue(
        new Promise<TopicMessage>((resolve) => {
          resolveDownload = resolve;
        })
      );
      const blockedMessage = { partition: 2, offset: 42, size: 2048 };
      const { rerender } = renderComponent({
        blockedMessage,
        fetchRequestId: 1,
      });

      await userEvent.click(
        screen.getByRole('button', { name: 'Open anyway' })
      );
      await userEvent.click(
        screen
          .getByRole('dialog', { name: 'Open large message?' })
          .querySelector('button:last-child') as HTMLButtonElement
      );

      rerender(
        <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
          <MessagesTable
            messages={[]}
            isFetching
            blockedMessage={blockedMessage}
            fetchRequestId={2}
          />
        </TopicActionsProvider>
      );
      resolveDownload({
        ...topicMessagePayload,
        partition: 2,
        offset: 42,
      });

      await waitFor(() =>
        expect(
          screen.getByText('Message blocked to protect this tab')
        ).toBeInTheDocument()
      );
      expect(
        screen.queryByText(topicMessagePayload.value || '')
      ).not.toBeInTheDocument();
    });

    it('keeps opened records during one fetch and resets them for the next request', async () => {
      (messagesApiClient.downloadTopicMessage as jest.Mock).mockResolvedValue({
        ...topicMessagePayload,
        partition: 2,
        offset: 42,
      });
      const blockedMessage = { partition: 2, offset: 42, size: 2048 };
      const { rerender } = renderComponent({
        blockedMessage,
        fetchRequestId: 1,
      });

      await userEvent.click(
        screen.getByRole('button', { name: 'Open anyway' })
      );
      await userEvent.click(
        screen
          .getByRole('dialog', { name: 'Open large message?' })
          .querySelector('button:last-child') as HTMLButtonElement
      );
      await screen.findByText(topicMessagePayload.value || '');

      rerender(
        <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
          <MessagesTable
            messages={[
              {
                ...topicMessagePayload,
                offset: 15,
                value: 'incremental SSE message',
              },
            ]}
            isFetching
            blockedMessage={blockedMessage}
            fetchRequestId={1}
          />
        </TopicActionsProvider>
      );
      expect(
        screen.getByText(topicMessagePayload.value || '')
      ).toBeInTheDocument();
      expect(screen.getByText('incremental SSE message')).toBeInTheDocument();

      rerender(
        <TopicActionsProvider openSidebarWithMessage={jest.fn()}>
          <MessagesTable
            messages={[]}
            isFetching
            blockedMessage={blockedMessage}
            fetchRequestId={2}
          />
        </TopicActionsProvider>
      );
      await waitFor(() =>
        expect(
          screen.queryByText(topicMessagePayload.value || '')
        ).not.toBeInTheDocument()
      );
      expect(
        screen.getByText('Message blocked to protect this tab')
      ).toBeInTheDocument();
    });

    it('should check the display of the loader element during loader', () => {
      renderComponent({ isFetching: true });
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('should render Messages table with data', () => {
    beforeEach(() => {
      renderComponent({ messages: mockTopicsMessages, isFetching: false });
    });

    it('should check the rendering of the messages', () => {
      expect(screen.queryByText(/No messages found/i)).not.toBeInTheDocument();
      if (mockTopicsMessages[0].value) {
        expect(
          screen.getByText(mockTopicsMessages[0].value)
        ).toBeInTheDocument();
      }
    });
  });

  describe('should save messages preview into localstorage', () => {
    beforeEach(() => {
      renderComponent({ messages: mockTopicsMessages, isFetching: false });
    });

    it('should save messages preview into localstorage', async () => {
      const previewButtons = screen.getAllByText('Preview');
      await userEvent.click(previewButtons[0]);
      await userEvent.type(screen.getByPlaceholderText('Field'), 'test1');
      await userEvent.type(screen.getByPlaceholderText('Json Path'), 'test2');
      await userEvent.click(screen.getByText('Save'));
      await userEvent.click(previewButtons[1]);
      await userEvent.type(screen.getByPlaceholderText('Field'), 'test3');
      await userEvent.type(screen.getByPlaceholderText('Json Path'), 'test4');
      await userEvent.click(screen.getByText('Save'));
      expect(
        global.localStorage.getItem(
          `${LOCAL_STORAGE_KEY_PREFIX}-message-preview`
        )
      ).toEqual(
        JSON.stringify({
          testTopic: {
            keyFilters: [{ field: 'test1', path: 'test2' }],
            contentFilters: [{ field: 'test3', path: 'test4' }],
          },
        })
      );
    });
  });
});
