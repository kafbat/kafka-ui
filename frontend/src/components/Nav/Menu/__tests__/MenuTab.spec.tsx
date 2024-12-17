import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MenuTab, { MenuTabProps } from 'components/Nav/Menu/MenuTab';
import { ServerStatus } from 'generated-sources';
import React from 'react';
import { render } from 'lib/testHelpers';

const testClusterName = 'My-Huge-Cluster';
const toggleClusterMenuMock = jest.fn();

describe('MenuTab component', () => {
  const setupWrapper = (props?: Partial<MenuTabProps>) => (
    <MenuTab
      status={ServerStatus.ONLINE}
      isOpen
      title={testClusterName}
      onClick={toggleClusterMenuMock}
      {...props}
    />
  );

  it('renders cluster name', () => {
    render(setupWrapper());
    expect(screen.getByText(testClusterName)).toBeInTheDocument();
  });

  it('renders correct status icon for online cluster', () => {
    render(setupWrapper());
    expect(screen.getByText(ServerStatus.ONLINE)).toBeInTheDocument();
  });

  it('renders correct status icon for offline cluster', () => {
    render(setupWrapper({ status: ServerStatus.OFFLINE }));
    expect(screen.getByText(ServerStatus.OFFLINE)).toBeInTheDocument();
  });

  it('handles onClick action', () => {
    const { baseElement } = render(setupWrapper());
    userEvent.click(baseElement);
    waitFor(() => expect(toggleClusterMenuMock).toHaveBeenCalled());
  });
});
