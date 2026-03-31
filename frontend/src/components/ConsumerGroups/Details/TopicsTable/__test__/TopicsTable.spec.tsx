import React from 'react';
import { clusterConsumerGroupDetailsPath } from 'lib/paths';
import { act, fireEvent, screen, within } from '@testing-library/react';
import { TopicsTable } from 'components/ConsumerGroups/Details/TopicsTable/TopicsTable';
import { render, WithRoute } from 'lib/testHelpers';
import { ConsumerGroupTopicPartition } from 'generated-sources';
import { consumerGroupPayload } from 'lib/fixtures/consumerGroups';

const clusterName = 'cluster1';

const renderComponent = (topicPartitions: ConsumerGroupTopicPartition[] = []) =>
  render(
    <WithRoute path={clusterConsumerGroupDetailsPath()}>
      <table>
        <tbody>
          <TopicsTable
            partitions={topicPartitions}
            topicsLagInfo={{ lags: {}, trends: {} }}
            partitionsLagInfo={{ lags: {}, trends: {} }}
          />
        </tbody>
      </table>
    </WithRoute>,
    {
      initialEntries: [
        clusterConsumerGroupDetailsPath(
          clusterName,
          consumerGroupPayload.groupId
        ),
      ],
    }
  );

describe('TopicContent', () => {
  it('renders empty table', () => {
    renderComponent();
    expect(screen.getByText('No topics')).toBeInTheDocument();
  });

  it('renders partitions only for the expanded topic', () => {
    renderComponent(consumerGroupPayload.partitions);

    const firstTopicPartition = consumerGroupPayload.partitions[0];
    const firstTopic = firstTopicPartition.topic;

    const expectedPartitions = consumerGroupPayload.partitions.filter(
      (partition) => partition.topic === firstTopic
    );

    expect(expectedPartitions.length).toBeGreaterThan(0);

    const topicLink = screen.getByRole('link', { name: firstTopic });
    expect(topicLink).toBeInTheDocument();

    const topicRow = topicLink.closest('tr');
    const expandButton = within(topicRow as HTMLElement).getByRole('button', {
      name: 'Expand row',
    });

    act(() => fireEvent.click(expandButton));

    const expandedRow = topicRow?.nextElementSibling as HTMLElement | null;
    expect(expandedRow).toBeTruthy();

    const expandedCell = expandedRow?.querySelector('td[colspan="5"]');
    expect(expandedCell).toHaveAttribute('colspan', '5');

    const nestedTable = within(expandedCell as HTMLElement).getByRole('table');
    const nestedRows = within(nestedTable)
      .getAllByRole('row')
      .filter((row) => row.querySelector('td'));

    const shownPartitions = nestedRows
      .map((row) => within(row).getAllByRole('cell')[0].textContent?.trim())
      .filter(Boolean);

    const expectedPartitionIds = expectedPartitions.map((partition) =>
      String(partition.partition)
    );

    expect(shownPartitions).toEqual(expectedPartitionIds);
  });
});
