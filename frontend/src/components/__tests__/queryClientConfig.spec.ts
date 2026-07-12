import { queryClientDefaultOptions } from 'components/App';

describe('queryClientDefaultOptions', () => {
  it('disables involuntary refetch on window focus and reconnect', () => {
    expect(queryClientDefaultOptions.queries?.refetchOnWindowFocus).toBe(false);
    expect(queryClientDefaultOptions.queries?.refetchOnReconnect).toBe(false);
  });

  it('keeps offlineFirst network mode', () => {
    expect(queryClientDefaultOptions.queries?.networkMode).toBe('offlineFirst');
  });
});
