import React from 'react';
import { waitFor, screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import { useTopicMessages } from 'lib/hooks/api/topicMessages';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';

const fetchEventSourceMock = jest.fn();

jest.mock('@microsoft/fetch-event-source', () => ({
  fetchEventSource: (...args: unknown[]) => fetchEventSourceMock(...args),
}));

const Probe = () => {
  const { consumptionStats } = useTopicMessages({
    clusterName: 'cluster-a',
    topicName: 'topic-a',
  });

  return (
    <div data-testid="stats">
      {String(consumptionStats?.messagesConsumed ?? 'none')}
    </div>
  );
};

describe('useTopicMessages', () => {
  beforeEach(() => {
    useMessageFiltersStore.setState({ nextCursor: undefined });
    fetchEventSourceMock.mockImplementation(
      async (
        _url: string,
        handlers: Record<string, (...args: unknown[]) => void>
      ) => {
        await handlers.onopen?.(new Response(null, { status: 200 }));

        handlers.onmessage?.({
          data: JSON.stringify({
            type: 'CONSUMING',
            consuming: {
              messagesConsumed: 1,
              bytesConsumed: 10,
              elapsedMs: 1,
            },
          }),
        } as MessageEvent);

        handlers.onmessage?.({
          data: JSON.stringify({
            type: 'DONE',
            consuming: {
              messagesConsumed: 2,
              bytesConsumed: 20,
              elapsedMs: 2,
            },
          }),
        } as MessageEvent);

        handlers.onclose?.();
      }
    );
  });

  it('updates consuming stats from DONE event', async () => {
    render(<Probe />);

    await waitFor(() => {
      expect(screen.getByTestId('stats')).toHaveTextContent('2');
    });
  });
});
