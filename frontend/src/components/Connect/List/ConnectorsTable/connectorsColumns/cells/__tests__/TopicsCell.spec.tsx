import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectorsPath } from 'lib/paths';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { connectors } from 'lib/fixtures/kafkaConnect';
import { CellContext } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';
import TopicsCell, {
  COLLAPSED_TOPICS_COUNT,
} from 'components/Connect/List/ConnectorsTable/connectorsColumns/cells/TopicsCell';

const clusterName = 'local';
const connectorTopics = Array.from({ length: 7 }, (_, index) => {
  return `topic-${index + 1}`;
});

const getCellProps = (topics: string[] | undefined) =>
  ({
    row: {
      original: {
        ...connectors[0],
        topics,
      },
    },
  }) as CellContext<FullConnectorInfo, unknown>;

describe('TopicsCell', () => {
  const renderComponent = (topics: string[] | undefined) =>
    render(
      <WithRoute path={clusterConnectorsPath()}>
        <TopicsCell {...getCellProps(topics)} />
      </WithRoute>,
      { initialEntries: [clusterConnectorsPath(clusterName)] }
    );

  it('renders a collapsed topic list by default and expands on demand', async () => {
    renderComponent(connectorTopics);

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

  it.each<[string, string[] | undefined]>([
    ['empty', []],
    ['undefined', undefined],
  ])('does not show a toggle button when topics are %s', (_label, topics) => {
    renderComponent(topics);

    expect(
      screen.queryByRole('button', { name: /show/i })
    ).not.toBeInTheDocument();
  });
});
