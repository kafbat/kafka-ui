import React from 'react';
import { render } from 'lib/testHelpers';
import Messages from 'components/Topics/Topic/Messages/Messages';
import { useTopicMessages } from 'lib/hooks/api/topicMessages';
import { screen } from '@testing-library/react';

const mockFilterComponents = 'mockFilterComponents';
const mockMessagesTable = 'mockMessagesTable';

jest.mock('lib/hooks/api/topicMessages', () => ({
  useTopicMessages: jest.fn(),
}));

jest.mock('components/Topics/Topic/Messages/MessagesTable', () => () => (
  <div>{mockMessagesTable}</div>
));

jest.mock('components/Topics/Topic/Messages/Filters/Filters', () => () => (
  <div>{mockFilterComponents}</div>
));

describe('Messages', () => {
  const renderComponent = () => {
    return render(<Messages />);
  };

  beforeEach(() => {
    (useTopicMessages as jest.Mock).mockImplementation(() => ({
      data: { messages: [], isFetching: false },
    }));
  });

  describe('component rendering default behavior with the search params', () => {
    beforeEach(() => {
      renderComponent();
    });

    it('should check if the filters are shown in the messages', () => {
      expect(screen.getByText(mockFilterComponents)).toBeInTheDocument();
    });

    it('should check if the table of messages are shown in the messages', () => {
      expect(screen.getByText(mockMessagesTable)).toBeInTheDocument();
    });
  });
});
