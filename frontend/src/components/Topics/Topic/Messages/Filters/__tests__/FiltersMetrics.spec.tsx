import React from 'react';
import { render } from 'lib/testHelpers';
import { PollingMode } from 'generated-sources';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FiltersMetrics, {
  FiltersMetricsProps,
} from 'components/Topics/Topic/Messages/Filters/FiltersMetrics';

const renderComponent = (props: Partial<FiltersMetricsProps> = {}) =>
  render(
    <FiltersMetrics
      mode={PollingMode.FROM_OFFSET}
      isFetching={false}
      abortFetchData={jest.fn()}
      consumptionStats={{ bytesConsumed: 0 }}
      {...props}
    />
  );

describe('FiltersMetrics', () => {
  describe('phase Message', () => {
    it('should check if the phase message phase is visible during and isFetching not tailing mode', () => {
      const phaseMessage = 'phaseMessage';
      renderComponent({
        mode: PollingMode.FROM_OFFSET,
        isFetching: true,
        phaseMessage,
      });
      expect(screen.getByText(phaseMessage)).toBeInTheDocument();
    });

    it('should check if the phase message phase is not visible during tailing modes and is fetching', () => {
      const phaseMessage = 'phaseMessage';
      renderComponent({
        mode: PollingMode.TAILING,
        phaseMessage,
        isFetching: true,
      });
      expect(screen.queryByText(phaseMessage)).not.toBeInTheDocument();
    });

    it('should check if the phase message phase is not visible during Live other modes and not fetching', () => {
      const phaseMessage = 'phaseMessage';
      renderComponent({
        mode: PollingMode.FROM_OFFSET,
        phaseMessage,
        isFetching: false,
      });
      expect(screen.queryByText(phaseMessage)).not.toBeInTheDocument();
    });
  });

  describe('consumptionStats data', () => {
    it('should check elapsed time is', () => {
      const elapsedMs = 2;
      renderComponent({ consumptionStats: { elapsedMs } });
      expect(screen.getByText(`${elapsedMs} ms`)).toBeInTheDocument();
    });

    it('should check elapsed time is 0 is negative data', () => {
      const elapsedMs = -2;
      renderComponent({ consumptionStats: { elapsedMs } });
      expect(screen.getByText(`0 ms`)).toBeInTheDocument();
    });

    it('should check messages consume text', () => {
      const messagesConsumed = 2;
      renderComponent({ consumptionStats: { messagesConsumed } });
      expect(
        screen.getByText(`${messagesConsumed} messages consumed`)
      ).toBeInTheDocument();
    });

    it('should check messages consume empty state', () => {
      renderComponent({ consumptionStats: {} });
      expect(screen.getByText(`messages consumed`)).toBeInTheDocument();
    });

    it('should check Bytes consumed text', () => {
      const bytesConsumed = 2;
      renderComponent({ consumptionStats: { bytesConsumed } });
      expect(screen.getByText(`${bytesConsumed} Bytes`)).toBeInTheDocument();
    });

    it('should check Bytes consumed text empty state', () => {
      renderComponent({ consumptionStats: {} });
      expect(screen.getByText('0 Bytes')).toBeInTheDocument();
    });

    it('should check filter error is visible', () => {
      const filterApplyErrors = 2;
      renderComponent({ consumptionStats: { filterApplyErrors } });
      expect(
        screen.getByText(`${filterApplyErrors} errors`)
      ).toBeInTheDocument();
    });

    it('should check Bytes consumed text empty state', () => {
      renderComponent({ consumptionStats: {} });
      expect(screen.queryByTitle('Errors')).not.toBeInTheDocument();
    });

    it('should check if the abortFetch Data is being called when clicked', async () => {
      const jestAbortMock = jest.fn();
      renderComponent({
        abortFetchData: jestAbortMock,
        isFetching: true,
        mode: PollingMode.TAILING,
      });

      const btn = screen.getByText(/stop loading/i);
      expect(btn).toBeInTheDocument();

      await userEvent.click(btn);
      expect(jestAbortMock).toHaveBeenCalledTimes(1);
    });
  });
});
