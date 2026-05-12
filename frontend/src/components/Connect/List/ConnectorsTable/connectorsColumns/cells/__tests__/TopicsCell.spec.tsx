import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectorsPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { connectors } from 'lib/fixtures/kafkaConnect';
import { CellContext } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';
import TopicsCell from 'components/Connect/List/ConnectorsTable/connectorsColumns/cells/TopicsCell';

const clusterName = 'local';
const connectorTopics = Array.from({ length: 7 }, (_, index) => {
  return `topic-${index + 1}`;
});

const cellProps = {
  row: {
    original: {
      ...connectors[0],
      topics: connectorTopics,
    },
  },
} as CellContext<FullConnectorInfo, unknown>;

describe('TopicsCell', () => {
  const renderComponent = () =>
    render(
      <WithRoute path={clusterConnectorsPath()}>
        <TopicsCell {...cellProps} />
      </WithRoute>,
      { initialEntries: [clusterConnectorsPath(clusterName)] }
    );

  it('renders a collapsed topic list by default and expands on demand', async () => {
    renderComponent();

    expect(screen.getByText('topic-5')).toBeInTheDocument();
    expect(screen.queryByText('topic-6')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Show 2 more' })
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show 2 more' }));

    expect(screen.getByText('topic-7')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Show less' })
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Show less' }));

    expect(screen.queryByText('topic-6')).not.toBeInTheDocument();
  });
});
