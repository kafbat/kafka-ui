import React from 'react';
import userEvent from '@testing-library/user-event';
import { screen } from '@testing-library/react';
import type { Table } from '@tanstack/react-table';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConsumerGroupDetailsPath } from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import { consumerGroupPayload } from 'lib/fixtures/consumerGroups';
import { exportTableCSV } from 'components/common/NewTable';
import { useConsumerGroupDetails } from 'lib/hooks/api/consumers';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { useGetConsumerGroupLagsInfo } from 'components/ConsumerGroups/Details/useGetConsumerGroupLagsInfo';
import Details from 'components/ConsumerGroups/Details/Details';

const clusterName = 'cluster1';
const consumerGroupID = consumerGroupPayload.groupId;

jest.mock('lib/hooks/api/consumers', () => ({
  ...jest.requireActual('lib/hooks/api/consumers'),
  useConsumerGroupDetails: jest.fn(),
  useDeleteConsumerGroupMutation: jest.fn(),
}));

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  ...jest.requireActual('lib/hooks/api/kafkaConnect'),
  useConnectors: jest.fn(),
}));

jest.mock(
  'components/ConsumerGroups/Details/useGetConsumerGroupLagsInfo',
  () => ({
    useGetConsumerGroupLagsInfo: jest.fn(),
  })
);

jest.mock('components/common/NewTable', () => ({
  __esModule: true,
  ...jest.requireActual('components/common/NewTable'),
  exportTableCSV: jest.fn(),
}));

const mockedUseConsumerGroupDetails = useConsumerGroupDetails as jest.Mock;
const mockedUseConnectors = useConnectors as jest.Mock;
const mockedUseGetConsumerGroupLagsInfo =
  useGetConsumerGroupLagsInfo as jest.Mock;
const mockedExportTableCSV = exportTableCSV as jest.Mock;

describe('Details CSV export', () => {
  beforeEach(() => {
    mockedUseConsumerGroupDetails.mockReturnValue({
      data: consumerGroupPayload,
      error: undefined,
      isSuccess: true,
      refetch: jest.fn(),
      isLoading: false,
    });
    mockedUseConnectors.mockReturnValue({ data: [] });
    mockedUseGetConsumerGroupLagsInfo.mockReturnValue({
      consumerGroupLagInfo: { lag: 0, trend: 'same' },
      topicsLagInfo: { lags: {}, trends: {} },
      partitionsLagInfo: { lags: {}, trends: {} },
    });
    mockedExportTableCSV.mockClear();
  });

  const renderComponent = () =>
    render(
      <ClusterContext.Provider
        value={{
          isReadOnly: false,
          hasKafkaConnectConfigured: false,
          hasSchemaRegistryConfigured: false,
          isTopicDeletionAllowed: true,
          ftsEnabled: false,
          ftsDefaultEnabled: false,
        }}
      >
        <WithRoute path={clusterConsumerGroupDetailsPath()}>
          <Details />
        </WithRoute>
      </ClusterContext.Provider>,
      {
        initialEntries: [
          clusterConsumerGroupDetailsPath(clusterName, consumerGroupID),
        ],
      }
    );

  it('exports topics and partitions via the dropdown', async () => {
    renderComponent();

    const exportBtn = screen.getByRole('button', { name: 'Export CSV' });
    await userEvent.click(exportBtn);

    const exportPartitions = await screen.findByText('Export partitions');
    await userEvent.click(exportPartitions);

    const [partitionsTable, partitionsOptions] =
      mockedExportTableCSV.mock.calls[0];
    expect(partitionsOptions).toEqual({ prefix: 'connector-partitions' });
    expect(
      (partitionsTable as Table<unknown>).getAllColumns().map((c) => c.id)
    ).toEqual(expect.arrayContaining(['partition', 'consumerLag', 'host']));

    mockedExportTableCSV.mockClear();

    await userEvent.click(exportBtn);
    const exportTopics = await screen.findByText('Export topics');
    await userEvent.click(exportTopics);

    const [topicsTable, topicsOptions] = mockedExportTableCSV.mock.calls[0];
    expect(topicsOptions).toEqual({ prefix: 'connector-topics' });
    expect(
      (topicsTable as Table<unknown>).getAllColumns().map((c) => c.id)
    ).toEqual(expect.arrayContaining(['topicName', 'consumerLag']));
  });
});
