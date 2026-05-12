import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectConnectorTopicsPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { connector } from 'lib/fixtures/kafkaConnect';
import { useConnector } from 'lib/hooks/api/kafkaConnect';
import Topics from 'components/Connect/Details/Topics/Topics';

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnector: jest.fn(),
}));

const clusterName = 'local';
const connectName = 'main-connect';
const connectorName = 'big-connector';
const connectorTopics = Array.from({ length: 12 }, (_, index) => {
  return `topic-${index + 1}`;
});

describe('Connector topics', () => {
  const renderComponent = () => {
    (useConnector as jest.Mock).mockImplementation(() => ({
      data: {
        ...connector,
        topics: connectorTopics,
      },
    }));

    return render(
      <WithRoute path={clusterConnectConnectorTopicsPath()}>
        <Topics />
      </WithRoute>,
      {
        initialEntries: [
          clusterConnectConnectorTopicsPath(
            clusterName,
            connectName,
            connectorName
          ),
        ],
      }
    );
  };

  it('renders a collapsed topic table by default and expands on demand', async () => {
    renderComponent();

    expect(screen.getByText('topic-10')).toBeInTheDocument();
    expect(screen.queryByText('topic-11')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Show all 12 topics' })
    ).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole('button', { name: 'Show all 12 topics' })
    );

    expect(screen.getByText('topic-12')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Show less' })
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show less' }));

    expect(screen.queryByText('topic-11')).not.toBeInTheDocument();
  });
});
