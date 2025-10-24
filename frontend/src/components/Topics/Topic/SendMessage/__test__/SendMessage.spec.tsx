import React from 'react';
import SendMessage from 'components/Topics/Topic/SendMessage/SendMessage';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterTopicPath } from 'lib/paths';
import { validateBySchema } from 'components/Topics/Topic/SendMessage/utils';
import { internalTopicPayload } from 'lib/fixtures/topics';
import { useSendMessage, useTopicDetails } from 'lib/hooks/api/topics';
import { useSerdes } from 'lib/hooks/api/topicMessages';
import { serdesPayload } from 'lib/fixtures/topicMessages';
import { MessageFormData } from 'lib/interfaces/message';

import Mock = jest.Mock;

jest.mock('json-schema-faker', () => ({
  generate: () => ({
    f1: -93251214,
    schema: 'enim sit in fugiat dolor',
    f2: 'deserunt culpa sunt',
  }),
  option: jest.fn(),
}));

jest.mock('components/Topics/Topic/SendMessage/utils', () => ({
  ...jest.requireActual('components/Topics/Topic/SendMessage/utils'),
  validateBySchema: jest.fn(),
}));

jest.mock('lib/errorHandling', () => ({
  ...jest.requireActual('lib/errorHandling'),
  showServerError: jest.fn(),
}));

jest.mock('lib/hooks/api/topics', () => ({
  useTopicDetails: jest.fn(),
  useSendMessage: jest.fn(),
}));

jest.mock('lib/hooks/api/topicMessages', () => ({
  useSerdes: jest.fn(),
}));

const clusterName = 'testCluster';
const topicPayloadMultiplePartitions = {
  ...internalTopicPayload,
  partitions: [
    {
      partition: 0,
      leader: 1,
      replicas: [{ broker: 1, leader: false, inSync: true }],
      offsetMax: 0,
      offsetMin: 0,
    },
    {
      partition: 1,
      leader: 1,
      replicas: [{ broker: 1, leader: false, inSync: true }],
      offsetMax: 0,
      offsetMin: 0,
    },
    {
      partition: 2,
      leader: 1,
      replicas: [{ broker: 1, leader: false, inSync: true }],
      offsetMax: 3,
      offsetMin: 0,
    },
    {
      partition: 3,
      leader: 1,
      replicas: [{ broker: 1, leader: false, inSync: true }],
      offsetMax: 0,
      offsetMin: 0,
    },
  ],
};
const topicName = topicPayloadMultiplePartitions.name;

const mockOnSubmit = jest.fn();

const renderComponent = async (
  messageData?: Partial<MessageFormData> | null
) => {
  const path = clusterTopicPath(clusterName, topicName);
  render(
    <WithRoute path={clusterTopicPath()}>
      <SendMessage closeSidebar={mockOnSubmit} messageData={messageData} />
    </WithRoute>,
    { initialEntries: [path] }
  );
};

const renderAndSubmitData = async (error: string[] = []) => {
  await renderComponent();
  await userEvent.click(screen.getAllByRole('listbox')[0]);

  await userEvent.click(screen.getAllByRole('option')[1]);

  (validateBySchema as Mock).mockImplementation(() => error);
  const submitButton = screen.getByRole('button', {
    name: 'Produce Message',
  });
  await waitFor(() => expect(submitButton).toBeEnabled());
  await userEvent.click(submitButton);
};

describe('SendMessage', () => {
  const sendTopicMessageMock = jest.fn();
  beforeEach(() => {
    (useSendMessage as jest.Mock).mockImplementation(() => ({
      mutateAsync: sendTopicMessageMock,
    }));
    (useTopicDetails as jest.Mock).mockImplementation(() => ({
      data: topicPayloadMultiplePartitions,
    }));
    (useSerdes as jest.Mock).mockImplementation(() => ({
      data: serdesPayload,
    }));
    (validateBySchema as jest.Mock).mockImplementation(() => []);
  });

  describe('when schema is fetched', () => {
    it('calls sendTopicMessage on submit', async () => {
      await renderAndSubmitData();
      expect(sendTopicMessageMock).toHaveBeenCalledTimes(1);
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
    });

    it('should check and view validation error message when is not valid', async () => {
      await renderAndSubmitData(['error']);
      expect(sendTopicMessageMock).not.toHaveBeenCalled();
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });
  });

  describe('when schema is empty', () => {
    it('renders if schema is not defined', async () => {
      await renderComponent();
      expect(screen.getAllByRole('textbox')[0].nodeValue).toBeNull();
    });
  });

  describe('when message data is provided', () => {
    const messageData: MessageFormData = {
      key: '123',
      content: 'test-content',
      headers: '{"header1": "value1"}',
      partition: 3,
      keySerde: 'Int32',
      valueSerde: 'String',
      keepContents: false,
    };

    it('should render form and produce message with prefilled values', async () => {
      await renderComponent(messageData);

      expect(
        screen.getByRole('listbox', {
          name: 'Partition',
        })
      ).toHaveTextContent('Partition #3');

      expect(
        screen.getByRole('listbox', {
          name: 'Key Serde',
        })
      ).toHaveTextContent(messageData.keySerde);

      expect(
        screen.getByRole('listbox', {
          name: 'Value Serde',
        })
      ).toHaveTextContent(messageData.valueSerde);

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledTimes(1);
      expect(sendTopicMessageMock).toHaveBeenCalledWith({
        headers: { header1: 'value1' },
        key: messageData.key,
        keySerde: messageData.keySerde,
        partition: messageData.partition,
        value: messageData.content,
        valueSerde: messageData.valueSerde,
      });
    });

    it('should combine partial form data with default values', async () => {
      const partialData: Partial<MessageFormData> = {
        content: 'only-content',
      };

      await renderComponent(partialData);

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledTimes(1);
      expect(sendTopicMessageMock).toHaveBeenCalledWith({
        headers: undefined,
        key: '{"f1":-93251214,"schema":"enim sit in fugiat dolor","f2":"deserunt culpa sunt"}',
        keySerde: 'Int32',
        partition: 0,
        value: partialData.content,
        valueSerde: 'Int64',
      });
    });

    it('should display correct partition in dropdown', async () => {
      await renderComponent(messageData);

      const partitionDropdown = screen.getByRole('listbox', {
        name: 'Partition',
      });
      expect(partitionDropdown).toHaveTextContent('Partition #3');
    });

    it('should close sidebar after submitting', async () => {
      (useSendMessage as jest.Mock).mockImplementation(() => ({
        mutateAsync: sendTopicMessageMock,
      }));

      await renderComponent(messageData);

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalled();
      });
    });
  });

  describe('when message data is null', () => {
    it('should render form with default values', async () => {
      await renderComponent();

      expect(
        screen.getByRole('listbox', {
          name: 'Partition',
        })
      ).toHaveTextContent('Partition #0');

      expect(
        screen.getByRole('listbox', {
          name: 'Key Serde',
        })
      ).toHaveTextContent('Int32');

      expect(
        screen.getByRole('listbox', {
          name: 'Value Serde',
        })
      ).toHaveTextContent('Int64');

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledTimes(1);
      expect(sendTopicMessageMock).toHaveBeenCalledWith({
        headers: undefined,
        key: '{"f1":-93251214,"schema":"enim sit in fugiat dolor","f2":"deserunt culpa sunt"}',
        keySerde: 'Int32',
        partition: 0,
        value:
          '{"f1":-93251214,"schema":"enim sit in fugiat dolor","f2":"deserunt culpa sunt"}',
        valueSerde: 'Int64',
      });
    });

    it('should submit with empty form successfully', async () => {
      (useSendMessage as jest.Mock).mockImplementation(() => ({
        mutateAsync: sendTopicMessageMock,
      }));

      await renderComponent(null);

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      await waitFor(() => {
        expect(sendTopicMessageMock).toHaveBeenCalled();
        expect(mockOnSubmit).toHaveBeenCalled();
      });
    });
  });
});
