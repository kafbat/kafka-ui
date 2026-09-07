import React, { PropsWithChildren } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LOCAL_STORAGE_KEY_PREFIX, MessagesFilterKeys } from 'lib/constants';
import { useMessagesFilters } from 'lib/hooks/useMessagesFilters';
import { useMessageFiltersStore } from 'lib/hooks/useMessageFiltersStore';

const clusterName = 'local';
const topicName = 'orders';
const resourceName = `${topicName}:${clusterName}`;
const fieldsStorageKey = `${LOCAL_STORAGE_KEY_PREFIX}-message-filters-fields`;

const collidingSavedFilter = {
  id: 'Non-heartbeat',
  value: 'record.partition == 1',
  filterCode: 'abcd1234',
};

jest.mock('lib/hooks/api/clusters', () => ({
  useClusterMessageFilters: () => ({
    data: [
      {
        id: 'cfg-non-heartbeat',
        displayName: 'Non-heartbeat',
        filterCode: 'has(record.value.after)',
        enabledByDefault: true,
      },
    ],
  }),
}));

const createWrapper =
  (search = 'limit=100&mode=LATEST') =>
  ({ children }: PropsWithChildren) => (
    <MemoryRouter
      initialEntries={[
        `/ui/clusters/${clusterName}/all-topics/${topicName}/messages?${search}`,
      ]}
    >
      <Routes>
        <Route
          path="/ui/clusters/:clusterName/all-topics/:topicName/messages"
          element={children}
        />
      </Routes>
    </MemoryRouter>
  );

describe('useMessagesFilters', () => {
  beforeEach(() => {
    localStorage.clear();
    useMessageFiltersStore.getState().removeAll();
  });

  it('does not reactivate a default filter after the user cleared it', async () => {
    localStorage.setItem(
      fieldsStorageKey,
      JSON.stringify({ [resourceName]: { activeFilterId: '' } })
    );

    const { result } = renderHook(() => useMessagesFilters(topicName), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.predefinedFilters).toHaveLength(1);
    });

    expect(result.current.smartFilter).toBeUndefined();
  });

  it('resolves a predefined filter when a saved filter shares the display name', async () => {
    useMessageFiltersStore.getState().save(collidingSavedFilter);

    const { result } = renderHook(() => useMessagesFilters(topicName), {
      wrapper: createWrapper(
        `limit=100&mode=LATEST&${MessagesFilterKeys.activeFilterId}=Non-heartbeat&${MessagesFilterKeys.smartFilterId}=cfg-non-heartbeat`
      ),
    });

    await waitFor(() => {
      expect(result.current.smartFilter?.predefined).toBe(true);
    });

    expect(result.current.smartFilter?.filterCode).toBe('cfg-non-heartbeat');
  });

  it('resolves a saved filter when a predefined filter shares the display name', async () => {
    useMessageFiltersStore.getState().save(collidingSavedFilter);

    const { result } = renderHook(() => useMessagesFilters(topicName), {
      wrapper: createWrapper(
        `limit=100&mode=LATEST&${MessagesFilterKeys.activeFilterId}=Non-heartbeat&${MessagesFilterKeys.smartFilterId}=abcd1234`
      ),
    });

    await waitFor(() => {
      expect(result.current.smartFilter?.predefined).toBeFalsy();
    });

    expect(result.current.smartFilter?.filterCode).toBe('abcd1234');
  });
});
