import React from 'react';
import { render } from 'lib/testHelpers';
import { screen, within } from '@testing-library/react';
import Statistics from 'components/Connect/List/Statistics/Statistics';
import { FullConnectorInfo } from 'generated-sources';
import { connectors } from 'lib/fixtures/kafkaConnect';

describe('Kafka Connect Connectors Statistics', () => {
  async function renderComponent({
    data,
    isLoading,
  }: {
    data: FullConnectorInfo[] | undefined;
    isLoading: boolean;
  }) {
    render(<Statistics connectors={data ?? []} isLoading={isLoading} />);
  }

  describe('when data loading', () => {
    let statistics: HTMLElement;
    beforeEach(() => {
      renderComponent({ data: undefined, isLoading: true });
      statistics = screen.getByRole('group');
    });

    it('renders statistic container elements', () => {
      expect(statistics).toBeInTheDocument();
    });

    describe('Connectors statistic', () => {
      let connectorStatisticsCell: HTMLElement;
      beforeEach(() => {
        connectorStatisticsCell = within(statistics).getByRole('cell', {
          name: 'Connectors',
        });
      });
      it('exists', () => {
        expect(connectorStatisticsCell).toBeInTheDocument();
      });

      it('shows loader', () => {
        const loader = within(connectorStatisticsCell).getByRole('status');
        expect(loader).toBeInTheDocument();
      });
    });
    describe('Tasks statistic', () => {
      let tasksStatisticsCell: HTMLElement;
      beforeEach(() => {
        tasksStatisticsCell = within(statistics).getByRole('cell', {
          name: 'Tasks',
        });
      });
      it('exists', () => {
        expect(tasksStatisticsCell).toBeInTheDocument();
      });

      it('shows loader', () => {
        const loader = within(tasksStatisticsCell).getByRole('status');
        expect(loader).toBeInTheDocument();
      });
    });
  });

  describe('when data is loaded', () => {
    let connectorsStatistic: HTMLElement;
    let tasksStatistics: HTMLElement;
    beforeEach(() => {
      renderComponent({ data: connectors, isLoading: false });
      [connectorsStatistic, tasksStatistics] = screen.getAllByRole('cell');
    });

    describe('Connectors statistics', () => {
      it('renders statistic', () => {
        expect(connectorsStatistic).toBeInTheDocument();

        const count = within(connectorsStatistic).getByText(3);
        expect(count).toBeInTheDocument();

        const alert = within(connectorsStatistic).getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('1');
      });
    });
    describe('Tasks statistics', () => {
      it('renders statistic', () => {
        expect(tasksStatistics).toBeInTheDocument();

        const count = within(tasksStatistics).getByText(5);
        expect(count).toBeInTheDocument();

        const alert = within(tasksStatistics).getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('1');
      });
    });
  });
});
