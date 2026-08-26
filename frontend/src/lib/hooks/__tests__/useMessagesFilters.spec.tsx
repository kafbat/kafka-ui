import React, { PropsWithChildren } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LOCAL_STORAGE_KEY_PREFIX } from 'lib/constants';
import { useMessagesFilters } from 'lib/hooks/useMessagesFilters';

const clusterName = 'local';
const topicName = 'orders';
const resourceName = `${topicName}:${clusterName}`;
const fieldsStorageKey = `${LOCAL_STORAGE_KEY_PREFIX}-message-filters-fields`;

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

describe('useMessagesFilters', () => {
  const wrapper = ({ children }: PropsWithChildren) => (
    <MemoryRouter
      initialEntries={[
        `/ui/clusters/${clusterName}/all-topics/${topicName}/messages?limit=100&mode=LATEST`,
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

  beforeEach(() => {
    localStorage.clear();
  });

  it('does not reactivate a default filter after the user cleared it', async () => {
    localStorage.setItem(
      fieldsStorageKey,
      JSON.stringify({ [resourceName]: { activeFilterId: '' } })
    );

    const { result } = renderHook(() => useMessagesFilters(topicName), {
      wrapper,
    });

    await waitFor(() => {
      expect(result.current.predefinedFilters).toHaveLength(1);
    });

    expect(result.current.smartFilter).toBeUndefined();
  });
});
