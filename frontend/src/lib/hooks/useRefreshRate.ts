import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import type { RefreshRateStorageKey } from 'components/common/RefreshRateSelect/RefreshRateSelect';

/**
 * Reads the user-selected auto-refresh rate (in seconds) persisted by
 * RefreshRateSelect and returns a value usable directly as a react-query
 * `refetchInterval`: milliseconds when polling is on, or `false` when "Off".
 */
export function useRefreshRate(storageKey: RefreshRateStorageKey) {
  const [rate] = useLocalStorage<number>(storageKey, 0);
  const refetchInterval: number | false = rate > 0 ? rate * 1000 : false;
  return { rate, refetchInterval };
}
