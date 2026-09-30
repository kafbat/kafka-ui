import React from 'react';
import { render } from 'lib/testHelpers';
import { PollingMode } from 'generated-sources';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FiltersMetrics, {
  FiltersMetricsProps,
} from 'components/Topics/Topic/Messages/Filters/FiltersMetrics';
import { usePaginateTopics } from 'lib/hooks/useMessagesFilters';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';

jest.mock('lib/hooks/useMessagesFilters', () => ({
  ...jest.requireActual('lib/hooks/useMessagesFilters'),
  usePaginateTopics: jest.fn(),
}));

const renderComponent = (props: Partial<FiltersMetricsProps> = {}) =>
  render(
    <FiltersMetrics
      mode={PollingMode.FROM_OFFSET}
      isFetching={false}
      abortFetchData={jest.fn()}
      onDownloadMessage={jest.fn()}
      consumptionStats={{ bytesConsumed: 0 }}
      {...props}
    />
  );

describe('FiltersMetrics', () => {
  const paginate = jest.fn();

  beforeEach(() => {
    jest.mocked(usePaginateTopics).mockReturnValue(paginate);
    useMessageFiltersStore.getState().setNextCursor('next-cursor');
  });

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
    it('explains that loading remains bounded', () => {
      renderComponent({
        consumptionStats: {
          bytesLimitReached: true,
          bytesLimit: 1024,
          messagesConsumed: 1,
        },
      });

      expect(
        screen.getByText('Loading paused at the 1 KB safety limit')
      ).toBeInTheDocument();
      expect(
        screen.getByText(
          '1 message loaded. More messages may be available. Each request uses the same limit.'
        )
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Load next 1 KB' })
      ).toBeEnabled();
    });

    it('opens manual download from the paused state', async () => {
      const onDownloadMessage = jest.fn();
      renderComponent({
        onDownloadMessage,
        consumptionStats: {
          bytesLimitReached: true,
          bytesLimit: 1024,
          messagesConsumed: 0,
        },
      });

      await userEvent.click(
        screen.getByRole('button', { name: 'Download by offset' })
      );
      expect(onDownloadMessage).toHaveBeenCalledTimes(1);
    });

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

    it('uses singular copy for one consumed message', () => {
      renderComponent({ consumptionStats: { messagesConsumed: 1 } });
      expect(screen.getByText('1 message consumed')).toBeInTheDocument();
    });

    it('should check messages consume empty state', () => {
      renderComponent({ consumptionStats: {} });
      expect(screen.getByText('0 messages consumed')).toBeInTheDocument();
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
