import { act, renderHook } from '@testing-library/react';
import { LOCAL_STORAGE_KEY_PREFIX, MessagesFilterKeys } from 'lib/constants';
import { useMessagesFiltersFields } from 'lib/hooks/useMessagesFiltersFields';

const resourceName = 'orders:local';
const fieldsStorageKey = `${LOCAL_STORAGE_KEY_PREFIX}-message-filters-fields`;

describe('useMessagesFiltersFields', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('keeps a cleared smart filter preference when URL only has limit and mode', () => {
    localStorage.setItem(
      fieldsStorageKey,
      JSON.stringify({ [resourceName]: { activeFilterId: '' } })
    );

    const { result } = renderHook(() => useMessagesFiltersFields(resourceName));

    expect(result.current.hasSmartFilterPreference).toBe(true);

    act(() => {
      result.current.initMessagesFiltersFields(
        new URLSearchParams('limit=100&mode=LATEST')
      );
    });

    expect(result.current.hasSmartFilterPreference).toBe(true);
    expect(
      JSON.parse(localStorage.getItem(fieldsStorageKey) || '{}')[resourceName]
        .activeFilterId
    ).toBe('');
  });

  it('replaces a stored preference when the URL selects a filter', () => {
    localStorage.setItem(
      fieldsStorageKey,
      JSON.stringify({ [resourceName]: { activeFilterId: '' } })
    );

    const { result } = renderHook(() => useMessagesFiltersFields(resourceName));

    act(() => {
      result.current.initMessagesFiltersFields(
        new URLSearchParams(
          `limit=100&mode=LATEST&${MessagesFilterKeys.activeFilterId}=Non-heartbeat&${MessagesFilterKeys.smartFilterId}=cfg-non-heartbeat`
        )
      );
    });

    expect(result.current.hasSmartFilterPreference).toBe(true);
    expect(
      JSON.parse(localStorage.getItem(fieldsStorageKey) || '{}')[resourceName]
        .activeFilterId
    ).toBe('Non-heartbeat');
  });
});
