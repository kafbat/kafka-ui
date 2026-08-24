import React from 'react';
import Overview from 'components/Connect/Details/Overview/Overview';
import { connector, tasks } from 'lib/fixtures/kafkaConnect';
import { fireEvent, screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import { ConnectorState } from 'generated-sources';

describe('Overview', () => {
  describe('when connector is loaded', () => {
    it('renders metrics', () => {
      render(<Overview tasks={tasks} connector={connector} />);

      expect(screen.getByText('Worker')).toBeInTheDocument();
      expect(
        screen.getByText(connector.status.workerId as string)
      ).toBeInTheDocument();

      expect(screen.getByText('Type')).toBeInTheDocument();
      expect(
        screen.getByText(connector.config['connector.class'] as string)
      ).toBeInTheDocument();

      expect(screen.getByText('Tasks Running')).toBeInTheDocument();
      expect(screen.getByText(2)).toBeInTheDocument();
      expect(screen.getByText('Tasks Failed')).toBeInTheDocument();
      expect(screen.getByText(1)).toBeInTheDocument();
    });

    it('opens modal when FAILED state is clicked and has connector trace', () => {
      const failedConnector = {
        ...connector,
        status: {
          ...connector.status,
          state: ConnectorState.FAILED,
          trace: 'Test error trace',
        },
      };

      render(<Overview tasks={[]} connector={failedConnector} />);

      const stateTag = screen.getByText('FAILED');
      expect(stateTag).toBeInTheDocument();
      expect(stateTag).toHaveStyle('cursor: pointer');

      fireEvent.click(stateTag);

      expect(screen.getByText('Connector Error Details')).toBeInTheDocument();
      expect(screen.getByText('Test error trace')).toBeInTheDocument();
    });

    it('does not open modal when FAILED state is clicked but no trace info', () => {
      const failedConnector = {
        ...connector,
        status: {
          ...connector.status,
          state: ConnectorState.FAILED,
          // No trace info
        },
      };

      render(<Overview tasks={[]} connector={failedConnector} />);

      const stateTag = screen.getByText('FAILED');
      expect(stateTag).toBeInTheDocument();
      expect(stateTag).toHaveStyle('cursor: default');

      fireEvent.click(stateTag);

      expect(
        screen.queryByText('Connector Error Details')
      ).not.toBeInTheDocument();
    });

    it('closes modal when close button is clicked', () => {
      const failedConnector = {
        ...connector,
        status: {
          ...connector.status,
          state: ConnectorState.FAILED,
          trace: 'Test error trace',
        },
      };

      render(<Overview tasks={[]} connector={failedConnector} />);

      const stateTag = screen.getByText('FAILED');
      fireEvent.click(stateTag);

      expect(screen.getByText('Connector Error Details')).toBeInTheDocument();

      const closeButton = screen.getByText('Close');
      fireEvent.click(closeButton);

      expect(
        screen.queryByText('Connector Error Details')
      ).not.toBeInTheDocument();
    });
  });
});
