import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import MessagesTable, {
  MessagesTableProps,
} from 'components/Topics/Topic/Messages/MessagesTable';
import { TopicMessage, TopicMessageTimestampTypeEnum } from 'generated-sources';
import { useIsLiveMode } from 'lib/hooks/useMessagesFilters';

export const topicMessagePayload: TopicMessage = {
  partition: 29,
  offset: 14,
  timestamp: new Date('2021-07-21T23:25:14.865Z'),
  timestampType: TopicMessageTimestampTypeEnum.CREATE_TIME,
  key: 'schema-registry',
  headers: {},
  content:
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

describe('MessagesTable', () => {
  const renderComponent = (props?: Partial<MessagesTableProps>) => {
    return render(
      <MessagesTable messages={[]} isFetching={false} {...props} />
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
      if (mockTopicsMessages[0].content) {
        expect(
          screen.getByText(mockTopicsMessages[0].content)
        ).toBeInTheDocument();
      }
    });
  });
});
