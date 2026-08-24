import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import Connect from 'components/Connect/Connect';
import { getNonExactPath, kafkaConnectPath } from 'lib/paths';
import { useConnects } from 'lib/hooks/api/kafkaConnect';

const ConnectCompText = {
  new: 'New Page',
  list: 'List Page',
  details: 'Details Page',
};

jest.mock('components/Connect/New/New', () => () => (
  <div>{ConnectCompText.new}</div>
));
jest.mock('components/Connect/List/ListPage', () => () => (
  <div>{ConnectCompText.list}</div>
));
jest.mock('components/Connect/Details/DetailsPage', () => () => (
  <div>{ConnectCompText.details}</div>
));
jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnects: jest.fn(),
}));

describe('Connect', () => {
  const renderComponent = (pathname: string, routePath: string) => {
    const useConnectsMock = useConnects as jest.Mock;
    useConnectsMock.mockReturnValue({
      data: [],
    });
    return render(
      <WithRoute path={getNonExactPath(routePath)}>
        <Connect />
      </WithRoute>,
      { initialEntries: [pathname] }
    );
  };

  it('renders header', () => {
    renderComponent(kafkaConnectPath('my-cluster'), kafkaConnectPath());

    const header = screen.getByRole('heading', { name: 'Kafka Connect' });
    expect(header).toBeInTheDocument();
  });

  it('renders navigation', () => {
    renderComponent(kafkaConnectPath('my-cluster'), kafkaConnectPath());

    const clusterNavigation = screen.getByRole('link', { name: 'Clusters' });
    expect(clusterNavigation).toBeInTheDocument();

    const connectorsNavigation = screen.getByRole('link', {
      name: 'Connectors',
    });
    expect(connectorsNavigation).toBeInTheDocument();
  });
});
