import React from 'react';
import { connectors } from 'lib/fixtures/kafkaConnect';
import ClusterContext, {
  ContextProps,
  initialValue,
} from 'components/contexts/ClusterContext';
import List from 'components/Connect/List/List';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectConnectorPath, clusterConnectorsPath } from 'lib/paths';
import {
  useConnectors,
  useDeleteConnector,
  useResetConnectorOffsets,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import { FullConnectorInfo } from 'generated-sources';
import { FilteredConnectorsProvider } from 'components/Connect/model/FilteredConnectorsProvider';

const mockedUsedNavigate = jest.fn();
const mockDelete = jest.fn();
const mockResetOffsets = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnectors: jest.fn(),
  useDeleteConnector: jest.fn(),
  useUpdateConnectorState: jest.fn(),
  useResetConnectorOffsets: jest.fn(),
}));

const clusterName = 'local';

const renderComponent = (
  contextValue: ContextProps = initialValue,
  data: FullConnectorInfo[] = connectors
) =>
  render(
    <ClusterContext.Provider value={contextValue}>
      <WithRoute path={clusterConnectorsPath()}>
        <FilteredConnectorsProvider>
          <List connectors={data} />
        </FilteredConnectorsProvider>
      </WithRoute>
    </ClusterContext.Provider>,
    { initialEntries: [clusterConnectorsPath(clusterName)] }
  );

describe('Connectors List', () => {
  describe('when the connectors are loaded', () => {
    beforeEach(() => {
      const restartConnector = jest.fn();
      (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
        mutateAsync: restartConnector,
      }));
    });

    it('renders', async () => {
      renderComponent();
      expect(screen.getByRole('table')).toBeInTheDocument();
      expect(screen.getAllByRole('row').length).toEqual(4);
    });

    it('opens broker when row clicked', async () => {
      renderComponent();
      screen.debug();
      expect(screen.getByText('hdfs-source-connector')).toHaveAttribute(
        'href',
        clusterConnectConnectorPath(
          clusterName,
          'first',
          'hdfs-source-connector'
        )
      );
    });
  });

  describe('when table is empty', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: [],
      }));
    });

    it('renders empty table', async () => {
      renderComponent(undefined, []);
      expect(screen.getByRole('table')).toBeInTheDocument();
      expect(
        screen.getByRole('row', { name: 'No connectors found' })
      ).toBeInTheDocument();
    });
  });

  describe('when delete modal is open', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: connectors,
      }));
      (useDeleteConnector as jest.Mock).mockImplementation(() => ({
        mutateAsync: mockDelete,
      }));
    });

    it('calls deleteConnector on confirm', async () => {
      renderComponent();
      const deleteButton = screen.getAllByText('Delete')[0];
      await waitFor(() => userEvent.click(deleteButton));

      const submitButton = screen.getAllByRole('button', {
        name: 'Confirm',
      })[0];
      await userEvent.click(submitButton);
      expect(mockDelete).toHaveBeenCalledWith();
    });

    it('closes the modal when cancel button is clicked', async () => {
      renderComponent();
      const deleteButton = screen.getAllByText('Delete')[0];
      await waitFor(() => userEvent.click(deleteButton));

      const cancelButton = screen.getAllByRole('button', {
        name: 'Cancel',
      })[0];
      await waitFor(() => userEvent.click(cancelButton));
      expect(cancelButton).not.toBeInTheDocument();
    });
  });

  describe('when reset connector offsets modal is open', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: connectors,
      }));
      (useResetConnectorOffsets as jest.Mock).mockImplementation(() => ({
        mutateAsync: mockResetOffsets,
      }));
    });

    it('calls resetConnectorOffsets on confirm', async () => {
      renderComponent();
      const resetButton = screen.getAllByText('Reset Offsets')[2];
      await waitFor(() => userEvent.click(resetButton));

      const submitButton = screen.getAllByRole('button', {
        name: 'Confirm',
      })[0];
      await userEvent.click(submitButton);
      expect(mockResetOffsets).toHaveBeenCalledWith();
    });

    it('closes the modal when cancel button is clicked', async () => {
      renderComponent();
      const resetButton = screen.getAllByText('Reset Offsets')[2];
      await waitFor(() => userEvent.click(resetButton));

      const cancelButton = screen.getAllByRole('button', {
        name: 'Cancel',
      })[0];
      await waitFor(() => userEvent.click(cancelButton));
      expect(cancelButton).not.toBeInTheDocument();
    });
  });
});
