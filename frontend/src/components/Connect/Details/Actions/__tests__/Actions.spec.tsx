import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { clusterConnectConnectorPath } from 'lib/paths';
import Actions from 'components/Connect/Details/Actions/Actions';
import { Connector, ConnectorAction, ConnectorState } from 'generated-sources';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  useConnector,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import { connector } from 'lib/fixtures/kafkaConnect';

function setConnectorStatus(con: Connector, state: ConnectorState) {
  return {
    ...con,
    status: {
      ...con,
      state,
    },
  };
}

const mockHistoryPush = jest.fn();
const deleteConnector = jest.fn();
const resetConnectorOffsets = jest.fn();
const cancelMock = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockHistoryPush,
}));

jest.mock('lib/hooks/api/kafkaConnect', () => ({
  useConnector: jest.fn(),
  useDeleteConnector: jest.fn(),
  useUpdateConnectorState: jest.fn(),
  useResetConnectorOffsets: jest.fn(),
}));

const expectActionButtonsExists = () => {
  expect(screen.getByText('Restart Connector')).toBeInTheDocument();
  expect(screen.getByText('Restart All Tasks')).toBeInTheDocument();
  expect(screen.getByText('Restart Failed Tasks')).toBeInTheDocument();
  expect(screen.getByText('Reset Offsets')).toBeInTheDocument();
  expect(screen.getByText('Delete')).toBeInTheDocument();
};
const afterClickDropDownButton = async () => {
  const dropDownButton = screen.getAllByRole('button');
  await userEvent.click(dropDownButton[1]);
};
const afterClickRestartButton = async () => {
  const dropDownButton = screen.getByText('Restart');
  await userEvent.click(dropDownButton);
};
describe('Actions', () => {
  afterEach(() => {
    mockHistoryPush.mockClear();
    deleteConnector.mockClear();
    cancelMock.mockClear();
    resetConnectorOffsets.mockClear();
  });

  describe('view', () => {
    const route = clusterConnectConnectorPath();
    const path = clusterConnectConnectorPath(
      'myCluster',
      'myConnect',
      'myConnector'
    );

    const renderComponent = () =>
      render(
        <WithRoute path={route}>
          <Actions />
        </WithRoute>,
        { initialEntries: [path] }
      );

    it('renders buttons when paused', async () => {
      (useConnector as jest.Mock).mockImplementation(() => ({
        data: setConnectorStatus(connector, ConnectorState.PAUSED),
      }));
      renderComponent();
      await afterClickRestartButton();
      expect(screen.getAllByRole('menuitem').length).toEqual(4);
      expect(screen.getByText('Resume')).toBeInTheDocument();
      expect(screen.queryByText('Pause')).not.toBeInTheDocument();
      expect(screen.queryByText('Stop')).not.toBeInTheDocument();
      expect(screen.queryByText('Reset Offsets')).toBeInTheDocument();
      expect(screen.getByText('Reset Offsets')).toBeDisabled();
      expectActionButtonsExists();
    });

    it('renders buttons when stopped', async () => {
      (useConnector as jest.Mock).mockImplementation(() => ({
        data: setConnectorStatus(connector, ConnectorState.PAUSED),
      }));
      renderComponent();
      await afterClickRestartButton();
      expect(screen.getAllByRole('menuitem').length).toEqual(4);
      expect(screen.getByText('Resume')).toBeInTheDocument();
      expect(screen.queryByText('Pause')).not.toBeInTheDocument();
      expect(screen.queryByText('Stop')).not.toBeInTheDocument();
      expect(screen.queryByText('Reset Offsets')).toBeInTheDocument();
      expect(screen.getByText('Reset Offsets')).toBeEnabled();
      expectActionButtonsExists();
    });

    it('renders buttons when failed', async () => {
      (useConnector as jest.Mock).mockImplementation(() => ({
        data: setConnectorStatus(connector, ConnectorState.FAILED),
      }));
      renderComponent();
      await afterClickRestartButton();
      expect(screen.getAllByRole('menuitem').length).toEqual(3);
      expect(screen.queryByText('Resume')).not.toBeInTheDocument();
      expect(screen.queryByText('Pause')).not.toBeInTheDocument();
      expect(screen.queryByText('Stop')).not.toBeInTheDocument();
      expect(screen.queryByText('Reset Offsets')).toBeInTheDocument();
      expect(screen.getByText('Reset Offsets')).toBeDisabled();
      expectActionButtonsExists();
    });

    it('renders buttons when unassigned', async () => {
      (useConnector as jest.Mock).mockImplementation(() => ({
        data: setConnectorStatus(connector, ConnectorState.UNASSIGNED),
      }));
      renderComponent();
      await afterClickRestartButton();
      expect(screen.getAllByRole('menuitem').length).toEqual(3);
      expect(screen.queryByText('Resume')).not.toBeInTheDocument();
      expect(screen.queryByText('Pause')).not.toBeInTheDocument();
      expect(screen.queryByText('Stop')).not.toBeInTheDocument();
      expect(screen.queryByText('Reset Offsets')).toBeInTheDocument();
      expect(screen.getByText('Reset Offsets')).toBeDisabled();
      expectActionButtonsExists();
    });

    it('renders buttons when running connector action', async () => {
      (useConnector as jest.Mock).mockImplementation(() => ({
        data: setConnectorStatus(connector, ConnectorState.RUNNING),
      }));
      renderComponent();
      await afterClickRestartButton();
      expect(screen.getAllByRole('menuitem').length).toEqual(4);
      expect(screen.queryByText('Resume')).not.toBeInTheDocument();
      expect(screen.getByText('Pause')).toBeInTheDocument();
      expect(screen.getByText('Stop')).toBeInTheDocument();
      expect(screen.queryByText('Reset Offsets')).toBeInTheDocument();
      expect(screen.getByText('Reset Offsets')).toBeDisabled();
      expectActionButtonsExists();
    });

    describe('mutations', () => {
      beforeEach(() => {
        (useConnector as jest.Mock).mockImplementation(() => ({
          data: setConnectorStatus(connector, ConnectorState.RUNNING),
        }));
      });

      it('opens confirmation modal when delete button clicked', async () => {
        renderComponent();
        await afterClickDropDownButton();
        await waitFor(async () =>
          userEvent.click(screen.getByRole('menuitem', { name: 'Delete' }))
        );
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      it('opens confirmation modal when reset offsets button clicked', async () => {
        renderComponent();
        await afterClickDropDownButton();
        await waitFor(async () =>
          userEvent.click(screen.getByRole('menuitem', { name: 'Reset Offsets' }))
        );
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });

      it('calls restartConnector when restart button clicked', async () => {
        const restartConnector = jest.fn();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: restartConnector,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(
          screen.getByRole('menuitem', { name: 'Restart Connector' })
        );
        expect(restartConnector).toHaveBeenCalledWith(ConnectorAction.RESTART);
      });

      it('calls restartAllTasks', async () => {
        const restartAllTasks = jest.fn();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: restartAllTasks,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(
          screen.getByRole('menuitem', { name: 'Restart All Tasks' })
        );
        expect(restartAllTasks).toHaveBeenCalledWith(
          ConnectorAction.RESTART_ALL_TASKS
        );
      });

      it('calls restartFailedTasks', async () => {
        const restartFailedTasks = jest.fn();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: restartFailedTasks,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(
          screen.getByRole('menuitem', { name: 'Restart Failed Tasks' })
        );
        expect(restartFailedTasks).toHaveBeenCalledWith(
          ConnectorAction.RESTART_FAILED_TASKS
        );
      });

      it('calls pauseConnector when pause button clicked', async () => {
        const pauseConnector = jest.fn();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: pauseConnector,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(screen.getByRole('menuitem', { name: 'Pause' }));
        expect(pauseConnector).toHaveBeenCalledWith(ConnectorAction.PAUSE);
      });

      it('calls stopConnector when stop button clicked', async () => {
        const stopConnector = jest.fn();
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: stopConnector,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(screen.getByRole('menuitem', { name: 'Stop' }));
        expect(stopConnector).toHaveBeenCalledWith(ConnectorAction.STOP);
      });

      it('calls resumeConnector when resume button clicked from PAUSED state', async () => {
        const resumeConnector = jest.fn();
        (useConnector as jest.Mock).mockImplementation(() => ({
          data: setConnectorStatus(connector, ConnectorState.PAUSED),
        }));
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: resumeConnector,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(screen.getByRole('menuitem', { name: 'Resume' }));
        expect(resumeConnector).toHaveBeenCalledWith(ConnectorAction.RESUME);
      });

      it('calls resumeConnector when resume button clicked from STOPPED state', async () => {
        const resumeConnector = jest.fn();
        (useConnector as jest.Mock).mockImplementation(() => ({
          data: setConnectorStatus(connector, ConnectorState.STOPPED),
        }));
        (useUpdateConnectorState as jest.Mock).mockImplementation(() => ({
          mutateAsync: resumeConnector,
        }));
        renderComponent();
        await afterClickRestartButton();
        await userEvent.click(screen.getByRole('menuitem', { name: 'Resume' }));
        expect(resumeConnector).toHaveBeenCalledWith(ConnectorAction.RESUME);
      });
    });
  });
});
