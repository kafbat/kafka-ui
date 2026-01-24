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
import { TopicSerdeSuggestion } from 'generated-sources';

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

const serdesPayloadWithSchemaRegistry: TopicSerdeSuggestion = {
  key: [
    { name: 'String', preferred: false },
    { name: 'Int32', preferred: true, schema: '{"type":"integer"}' },
    {
      name: 'SchemaRegistry',
      preferred: false,
      subjects: ['user-key', 'order-key'],
    },
  ],
  value: [
    { name: 'String', preferred: false },
    { name: 'Int64', preferred: true, schema: '{"type":"integer"}' },
    {
      name: 'SchemaRegistry',
      preferred: false,
      subjects: ['user-value', 'order-value'],
    },
  ],
};

const renderComponent = async (
  messageData?: Partial<MessageFormData> | null,
  searchParams?: string
) => {
  const basePath = clusterTopicPath(clusterName, topicName);
  const path = searchParams ? `${basePath}?${searchParams}` : basePath;
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

  describe('URL search params', () => {
    it('should use keySerde from URL params', async () => {
      await renderComponent(null, 'keySerde=String');

      expect(
        screen.getByRole('listbox', {
          name: 'Key Serde',
        })
      ).toHaveTextContent('String');
    });

    it('should use valueSerde from URL params', async () => {
      await renderComponent(null, 'valueSerde=String');

      expect(
        screen.getByRole('listbox', {
          name: 'Value Serde',
        })
      ).toHaveTextContent('String');
    });

    it('should use both keySerde and valueSerde from URL params', async () => {
      await renderComponent(null, 'keySerde=String&valueSerde=String');

      expect(
        screen.getByRole('listbox', {
          name: 'Key Serde',
        })
      ).toHaveTextContent('String');

      expect(
        screen.getByRole('listbox', {
          name: 'Value Serde',
        })
      ).toHaveTextContent('String');
    });

    it('should submit with serde values from URL params', async () => {
      await renderComponent(null, 'keySerde=String&valueSerde=String');

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledWith(
        expect.objectContaining({
          keySerde: 'String',
          valueSerde: 'String',
        })
      );
    });

    it('should prefer URL params over messageData due to useEffect', async () => {
      // Note: URL params take precedence because the useEffect runs after mount
      // and explicitly sets the serde values from URL params
      const messageData: Partial<MessageFormData> = {
        keySerde: 'Int32',
        valueSerde: 'Int64',
      };
      await renderComponent(messageData, 'keySerde=String&valueSerde=String');

      // URL params should win over messageData
      expect(
        screen.getByRole('listbox', {
          name: 'Key Serde',
        })
      ).toHaveTextContent('String');

      expect(
        screen.getByRole('listbox', {
          name: 'Value Serde',
        })
      ).toHaveTextContent('String');
    });
  });

  describe('subject fields with SchemaRegistry', () => {
    beforeEach(() => {
      (useSerdes as jest.Mock).mockImplementation(() => ({
        data: serdesPayloadWithSchemaRegistry,
      }));
    });

    it('should show Key Subject field when SchemaRegistry is selected for key', async () => {
      await renderComponent();

      // Select SchemaRegistry for key
      await userEvent.click(
        screen.getByRole('listbox', { name: 'Key Serde' })
      );
      await userEvent.click(
        screen.getByRole('option', { name: 'SchemaRegistry' })
      );

      // Key Subject field should appear (identified by its label text)
      await waitFor(() => {
        expect(screen.getByText('Key Subject')).toBeInTheDocument();
      });
    });

    it('should show Value Subject field when SchemaRegistry is selected for value', async () => {
      await renderComponent();

      // Select SchemaRegistry for value
      await userEvent.click(
        screen.getByRole('listbox', { name: 'Value Serde' })
      );
      await userEvent.click(
        screen.getByRole('option', { name: 'SchemaRegistry' })
      );

      // Value Subject field should appear (identified by its label text)
      await waitFor(() => {
        expect(screen.getByText('Value Subject')).toBeInTheDocument();
      });
    });

    it('should not show subject fields when non-SchemaRegistry serde is selected', async () => {
      await renderComponent();

      // By default, Int32/Int64 are selected (preferred), not SchemaRegistry
      expect(screen.queryByText('Key Subject')).not.toBeInTheDocument();
      expect(screen.queryByText('Value Subject')).not.toBeInTheDocument();
    });

    it('should submit with keySubject when SchemaRegistry key is selected', async () => {
      await renderComponent();

      // Select SchemaRegistry for key
      await userEvent.click(screen.getByRole('listbox', { name: 'Key Serde' }));
      await userEvent.click(
        screen.getByRole('option', { name: 'SchemaRegistry' })
      );

      // Wait for subject field to appear and type a subject
      await waitFor(() => {
        expect(screen.getByText('Key Subject')).toBeInTheDocument();
      });

      const keySubjectInput = screen.getByPlaceholderText('Search subjects...');
      await userEvent.type(keySubjectInput, 'user-key');

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledWith(
        expect.objectContaining({
          keySerde: 'SchemaRegistry',
          keySubject: 'user-key',
        })
      );
    });

    it('should submit with valueSubject when SchemaRegistry value is selected', async () => {
      await renderComponent();

      // Select SchemaRegistry for value
      await userEvent.click(
        screen.getByRole('listbox', { name: 'Value Serde' })
      );
      await userEvent.click(
        screen.getByRole('option', { name: 'SchemaRegistry' })
      );

      // Wait for subject field to appear and type a subject
      await waitFor(() => {
        expect(screen.getByText('Value Subject')).toBeInTheDocument();
      });

      const valueSubjectInputs =
        screen.getAllByPlaceholderText('Search subjects...');
      const valueSubjectInput =
        valueSubjectInputs[valueSubjectInputs.length - 1];
      await userEvent.type(valueSubjectInput, 'user-value');

      const submitButton = screen.getByRole('button', {
        name: 'Produce Message',
      });
      await userEvent.click(submitButton);

      expect(sendTopicMessageMock).toHaveBeenCalledWith(
        expect.objectContaining({
          valueSerde: 'SchemaRegistry',
          valueSubject: 'user-value',
        })
      );
    });
  });
});
