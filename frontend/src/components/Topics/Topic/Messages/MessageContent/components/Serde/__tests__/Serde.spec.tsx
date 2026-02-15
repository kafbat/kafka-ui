import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterTopicPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import Serde from 'components/Topics/Topic/Messages/MessageContent/components/Serde/Serde';
import { TopicMessage } from 'generated-sources';

const clusterName = 'local';
const topicName = 'topic';

describe('Serde', () => {
  const renderComponent = ({
    serde,
    properties,
  }: {
    serde?: string;
    properties?: TopicMessage['keyDeserializeProperties'];
  }) => {
    const path = clusterTopicPath(clusterName, topicName);
    return render(
      <WithRoute path={clusterTopicPath(clusterName, topicName)}>
        <Serde title="Key Serde" serde={serde} properties={properties} />
      </WithRoute>,
      { initialEntries: [path] }
    );
  };

  describe('for SchemaRegistry serde', () => {
    it('renders link to subject', () => {
      renderComponent({
        serde: 'SchemaRegistry',
        properties: {
          type: 'AVRO',
          id: 0,
          subjects: ['avrotest-value'],
        },
      });
      const link = screen.queryByRole('link', { name: 'Schema link' });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute(
        'href',
        '/ui/clusters/:clusterName/schemas/avrotest-value'
      );
    });
  });

  describe('for other serde', () => {
    it('doesnt render link', () => {
      renderComponent({ serde: 'String' });
      const link = screen.queryByRole('link', { name: 'Schema link' });
      expect(link).not.toBeInTheDocument();
    });
  });
});
