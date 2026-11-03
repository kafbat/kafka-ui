import { renderHook } from '@testing-library/react';
import { useRefreshRate } from 'lib/hooks/useRefreshRate';

describe('useRefreshRate', () => {
  beforeEach(() => localStorage.clear());

  it('returns refetchInterval false when rate is off (default 0)', () => {
    const { result } = renderHook(() => useRefreshRate('topics-refresh-rate'));
    expect(result.current.rate).toBe(0);
    expect(result.current.refetchInterval).toBe(false);
  });

  it('converts a stored rate in seconds to milliseconds', () => {
    localStorage.setItem('kafbat-ui-topics-refresh-rate', '5');
    const { result } = renderHook(() => useRefreshRate('topics-refresh-rate'));
    expect(result.current.rate).toBe(5);
    expect(result.current.refetchInterval).toBe(5000);
  });
});
