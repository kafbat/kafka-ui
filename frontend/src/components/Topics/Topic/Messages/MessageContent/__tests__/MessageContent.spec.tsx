import { TextEncoder } from 'util';

import React from 'react';
import { screen } from '@testing-library/react';
import MessageContent, {
  MessageContentProps,
} from 'components/Topics/Topic/Messages/MessageContent/MessageContent';
import { TopicMessageTimestampTypeEnum } from 'generated-sources';
import userEvent from '@testing-library/user-event';
import { render, WithRoute } from 'lib/testHelpers';
import { theme } from 'theme/theme';
import { clusterTopicPath } from 'lib/paths';

const clusterName = 'test-cluster';
const topicName = 'test-topic';

const setupWrapper = (props?: Partial<MessageContentProps>) => {
  return (
    <WithRoute path={clusterTopicPath()}>
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
            topicName={topicName}
            {...props}
          />
        </tbody>
      </table>
    </WithRoute>
  );
};

global.TextEncoder = TextEncoder;

const renderComponent = (props?: Partial<MessageContentProps>) => {
  return render(setupWrapper(props), {
    initialEntries: [clusterTopicPath(clusterName, topicName)],
  });
};

describe('MessageContent screen', () => {
  beforeEach(() => {
    renderComponent();
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

  describe('Schema ID display', () => {
    it('renders Value Schema ID when valueDeserializeProperties.schemaId exists', () => {
      renderComponent({
        valueDeserializeProperties: { schemaId: 123, type: 'AVRO' },
      });
      expect(screen.getByText('Value Schema ID')).toBeInTheDocument();
      expect(screen.getByText('123')).toBeInTheDocument();
      expect(screen.getByText('Type: AVRO')).toBeInTheDocument();
    });

    it('renders Key Schema ID when keyDeserializeProperties.schemaId exists', () => {
      renderComponent({
        keyDeserializeProperties: { schemaId: 456, type: 'PROTOBUF' },
      });
      expect(screen.getByText('Key Schema ID')).toBeInTheDocument();
      expect(screen.getByText('456')).toBeInTheDocument();
      expect(screen.getByText('Type: PROTOBUF')).toBeInTheDocument();
    });

    it('renders both Key and Value Schema IDs when both exist', () => {
      renderComponent({
        valueDeserializeProperties: { schemaId: 1, type: 'AVRO' },
        keyDeserializeProperties: { schemaId: 2, type: 'JSON' },
      });
      expect(screen.getByText('Value Schema ID')).toBeInTheDocument();
      expect(screen.getByText('Key Schema ID')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('does not render schema ID sections when deserializeProperties is undefined', () => {
      renderComponent();
      expect(screen.queryByText('Value Schema ID')).not.toBeInTheDocument();
      expect(screen.queryByText('Key Schema ID')).not.toBeInTheDocument();
    });

    it('does not render schema ID sections when schemaId is not present in properties', () => {
      renderComponent({
        valueDeserializeProperties: { type: 'AVRO' },
        keyDeserializeProperties: { type: 'JSON' },
      });
      expect(screen.queryByText('Value Schema ID')).not.toBeInTheDocument();
      expect(screen.queryByText('Key Schema ID')).not.toBeInTheDocument();
    });

    it('renders schema ID as a link to the schema page', () => {
      renderComponent({
        valueDeserializeProperties: { schemaId: 123 },
      });
      const link = screen.getByRole('link', { name: '123' });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute(
        'href',
        `/ui/clusters/${clusterName}/schemas/${topicName}-value`
      );
    });
  });
});
