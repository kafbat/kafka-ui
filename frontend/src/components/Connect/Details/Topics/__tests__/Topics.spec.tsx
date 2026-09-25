import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectConnectorTopicsPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { connector } from 'lib/fixtures/kafkaConnect';
import { useConnector } from 'lib/hooks/api/kafkaConnect';
import Topics, {
  COLLAPSED_TOPICS_COUNT,
} from 'components/Connect/Details/Topics/Topics';

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
  const renderComponent = (topics: string[] | undefined) => {
    (useConnector as jest.Mock).mockImplementation(() => ({
      data: {
        ...connector,
        topics,
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
    renderComponent(connectorTopics);

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

  it('does not show a toggle button when topics do not exceed the collapsed count', () => {
    const topics = Array.from(
      { length: COLLAPSED_TOPICS_COUNT },
      (_, index) => {
        return `topic-${index + 1}`;
      }
    );

    renderComponent(topics);

    expect(
      screen.getByText(`topic-${COLLAPSED_TOPICS_COUNT}`)
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /show/i })
    ).not.toBeInTheDocument();
  });

  it('renders an empty state without a toggle button when topics are undefined', () => {
    renderComponent(undefined);

    expect(screen.getByText('No topics found')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /show/i })
    ).not.toBeInTheDocument();
  });
});
