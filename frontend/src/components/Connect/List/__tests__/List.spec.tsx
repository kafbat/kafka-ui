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
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';

const mockedUsedNavigate = jest.fn();
const mockDelete = jest.fn();
const mockUpdate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnectors: jest.fn(),
  useDeleteConnector: jest.fn(),
  useUpdateConnectorState: jest.fn(),
}));

const clusterName = 'local';

const getButtonByName = (name: string) => screen.getByRole('button', { name });

const renderComponent = (contextValue: ContextProps = initialValue) =>
  render(
    <ClusterContext.Provider value={contextValue}>
      <WithRoute path={clusterConnectorsPath()}>
        <List />
      </WithRoute>
    </ClusterContext.Provider>,
    { initialEntries: [clusterConnectorsPath(clusterName)] }
  );

describe('Connectors List', () => {
  describe('when the connectors are loaded', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: connectors,
      }));
      const restartConnector = jest.fn();
      (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
        mutateAsync: restartConnector,
      }));
    });

    it('renders', async () => {
      renderComponent();
      expect(screen.getByRole('table')).toBeInTheDocument();
      expect(screen.getAllByRole('row').length).toEqual(3);
    });

    it('connector link has correct href', async () => {
      renderComponent();
      const connectorTitleLink = screen.getByTitle(
        connectors[0].name
      ) as HTMLAnchorElement;
      expect(connectorTitleLink).toHaveAttribute(
        'href',
        clusterConnectConnectorPath(
          clusterName,
          connectors[0].connect,
          connectors[0].name
        )
      );
    });

    describe('Batch actions bar', () => {
      beforeEach(() => {
        renderComponent();
        expect(screen.getAllByRole('checkbox').length).toEqual(3);
        expect(screen.getAllByRole('checkbox')[1]).toBeEnabled();
        expect(screen.getAllByRole('checkbox')[2]).toBeEnabled();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: mockUpdate,
        }));
        (useDeleteConnector as jest.Mock).mockImplementation(() => ({
          mutateAsync: mockDelete,
        }));
      });
      describe('when only one connector is selected', () => {
        beforeEach(async () => {
          await userEvent.click(screen.getAllByRole('checkbox')[1]);
        });
        it('renders batch actions bar', () => {
          expect(getButtonByName('Pause Connectors')).toBeEnabled();
          expect(getButtonByName('Resume Connectors')).toBeEnabled();
          expect(getButtonByName('Restart Connectors')).toBeEnabled();
          expect(getButtonByName('Restart All Tasks')).toBeEnabled();
          expect(getButtonByName('Restart Failed Tasks')).toBeEnabled();
          expect(getButtonByName('Remove Connectors')).toBeEnabled();
        });
        it('handels pause button click', async () => {
          const button = getButtonByName('Pause Connectors');
          await userEvent.click(button);
          expect(
            screen.getByText(
              'Are you sure you want to pause selected connectors?'
            )
          ).toBeInTheDocument();
          const confirmBtn = getButtonByName('Confirm');
          expect(confirmBtn).toBeInTheDocument();
          expect(mockUpdate).not.toHaveBeenCalled();
          await userEvent.click(confirmBtn);
          expect(mockUpdate).toHaveBeenCalledTimes(1);
          expect(screen.getAllByRole('checkbox')[1]).not.toBeChecked();
        });
      });
      describe('when more then one connectors are selected', () => {
        beforeEach(async () => {
          await userEvent.click(screen.getAllByRole('checkbox')[1]);
          await userEvent.click(screen.getAllByRole('checkbox')[2]);
        });
        it('renders batch actions bar', () => {
          expect(getButtonByName('Pause Connectors')).toBeEnabled();
          expect(getButtonByName('Resume Connectors')).toBeEnabled();
          expect(getButtonByName('Restart Connectors')).toBeEnabled();
          expect(getButtonByName('Restart All Tasks')).toBeEnabled();
          expect(getButtonByName('Restart Failed Tasks')).toBeEnabled();
          expect(getButtonByName('Remove Connectors')).toBeEnabled();
        });
        it('handels delete button click', async () => {
          const button = getButtonByName('Remove Connectors');
          await userEvent.click(button);
          expect(
            screen.getByText(
              'Are you sure you want to delete selected connectors?'
            )
          ).toBeInTheDocument();
          const confirmBtn = getButtonByName('Confirm');
          expect(confirmBtn).toBeInTheDocument();
          expect(mockDelete).not.toHaveBeenCalled();
          await userEvent.click(confirmBtn);
          expect(mockDelete).toHaveBeenCalledTimes(2);
          expect(screen.getAllByRole('checkbox')[1]).not.toBeChecked();
          expect(screen.getAllByRole('checkbox')[2]).not.toBeChecked();
        });
      });
    });
  });

  describe('when table is empty', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: [],
      }));
    });

    it('renders empty table', async () => {
      renderComponent();
      expect(screen.getByRole('table')).toBeInTheDocument();
      expect(
        screen.getByRole('row', { name: 'No connectors found' })
      ).toBeInTheDocument();
    });
  });

  describe('when remove connector modal is open', () => {
    beforeEach(() => {
      (useConnectors as jest.Mock).mockImplementation(() => ({
        data: connectors,
      }));
      (useDeleteConnector as jest.Mock).mockImplementation(() => ({
        mutateAsync: mockDelete,
      }));
    });

    it('calls removeConnector on confirm', async () => {
      renderComponent();
      const removeButton = screen.getAllByText('Remove Connector')[0];
      await waitFor(() => userEvent.click(removeButton));

      const submitButton = screen.getAllByRole('button', {
        name: 'Confirm',
      })[0];
      await userEvent.click(submitButton);
      expect(mockDelete).toHaveBeenCalledWith({
        props: {
          clusterName,
          connectName: connectors[0].connect,
          connectorName: connectors[0].name,
        },
      });
    });

    it('closes the modal when cancel button is clicked', async () => {
      renderComponent();
      const removeButton = screen.getAllByText('Remove Connector')[0];
      await waitFor(() => userEvent.click(removeButton));

      const cancelButton = screen.getAllByRole('button', {
        name: 'Cancel',
      })[0];
      await waitFor(() => userEvent.click(cancelButton));
      expect(cancelButton).not.toBeInTheDocument();
    });
  });
});
