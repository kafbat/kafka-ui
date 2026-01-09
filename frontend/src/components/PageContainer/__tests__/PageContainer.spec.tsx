import React from 'react';
import { screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import PageContainer from 'components/PageContainer/PageContainer';
import { useClusters } from 'lib/hooks/api/clusters';
import { Cluster, ControllerType, ServerStatus } from 'generated-sources';
import { useGetUserInfo } from 'lib/hooks/api/roles';

jest.mock('components/Version/Version', () => () => <div>Version</div>);

interface DataType {
  data: Cluster[] | undefined;
}

jest.mock('lib/hooks/api/clusters');
jest.mock('lib/hooks/api/roles');
const mockedNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedNavigate,
}));
describe('Page Container', () => {
  const renderComponent = (hasDynamicConfig: boolean, data: DataType) => {
    const useClustersMock = useClusters as jest.Mock;
    useClustersMock.mockReturnValue(data);
    const useGetUserInfoMock = useGetUserInfo as jest.Mock;
    useGetUserInfoMock.mockReturnValue({
      data: { rbacEnabled: false },
    });
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: jest.fn().mockImplementation(() => ({
        matches: false,
        addListener: jest.fn(),
      })),
    });
    render(
      <PageContainer>
        <div>child</div>
      </PageContainer>,
      {
        globalSettings: { hasDynamicConfig },
      }
    );
  };

  it('render the inner container', async () => {
    renderComponent(false, { data: undefined });
    expect(screen.getByText('child')).toBeInTheDocument();
  });

  describe('Redirect to the Wizard page', () => {
    it('redirects to new cluster configuration page if there are no clusters and dynamic config is enabled', async () => {
      renderComponent(true, { data: [] });

      expect(mockedNavigate).toHaveBeenCalled();
    });

    it('should not navigate to new cluster config page when there are clusters', async () => {
      renderComponent(true, {
        data: [
          {
            name: 'Cluster 1',
            status: ServerStatus.ONLINE,
            controller: ControllerType.KRAFT,
          },
        ],
      });

      expect(mockedNavigate).not.toHaveBeenCalled();
    });

    it('should not navigate to new cluster config page when there are no clusters and hasDynamicConfig is false', async () => {
      renderComponent(false, {
        data: [],
      });

      expect(mockedNavigate).not.toHaveBeenCalled();
    });
  });
});
