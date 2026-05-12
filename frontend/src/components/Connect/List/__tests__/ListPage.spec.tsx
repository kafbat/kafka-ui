import React from 'react';
import { connects } from 'lib/fixtures/kafkaConnect';
import ClusterContext, {
  ContextProps,
  initialValue,
} from 'components/contexts/ClusterContext';
import ListPage from 'components/Connect/List/ListPage';
import { screen } from '@testing-library/react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectorsPath } from 'lib/paths';
import { useConnectors, useConnects } from 'lib/hooks/api/kafkaConnect';

jest.mock('components/Connect/List/ConnectorsTable/ConnectorsTable', () => ({
  ConnectorsTable: () => <div>Connectors List</div>,
}));

jest.mock('components/Connect/List/Statistics/Statistics', () => () => (
  <div>Statistics</div>
));

jest.mock('components/common/Fts/Fts', () => () => <div>Fts</div>);

jest.mock('components/common/Fts/useFts', () => () => ({
  isFtsEnabled: false,
}));

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnectors: jest.fn(),
  useConnects: jest.fn(),
}));

jest.mock('components/common/Icons/SpinnerIcon', () => () => 'progressbar');

const clusterName = 'local';

describe('Connectors List Page', () => {
  beforeEach(() => {
    (useConnectors as jest.Mock).mockImplementation(() => ({
      isLoading: false,
      isRefetching: false,
      isSuccess: true,
      error: null,
      data: [],
      refetch: jest.fn(),
    }));

    (useConnects as jest.Mock).mockImplementation(() => ({
      isLoading: false,
      isRefetching: false,
      isSuccess: true,
      error: null,
      data: connects,
      refetch: jest.fn(),
    }));
  });

  const renderComponent = async (contextValue: ContextProps = initialValue) =>
    render(
      <ClusterContext.Provider value={contextValue}>
        <WithRoute path={clusterConnectorsPath()}>
          <ListPage />
        </WithRoute>
      </ClusterContext.Provider>,
      { initialEntries: [clusterConnectorsPath(clusterName)] }
    );

  it('renders search input', async () => {
    await renderComponent();
    expect(
      screen.getByPlaceholderText('Search by Connect Name, Status or Type')
    ).toBeInTheDocument();
  });

  it('renders list', async () => {
    await renderComponent();
    expect(screen.getByText('Connectors List')).toBeInTheDocument();
  });
});
