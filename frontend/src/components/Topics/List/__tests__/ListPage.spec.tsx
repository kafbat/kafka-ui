import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import ClusterContext from 'components/contexts/ClusterContext';
import userEvent from '@testing-library/user-event';
import { clusterTopicsPath } from 'lib/paths';
import ListPage from 'components/Topics/List/ListPage';

const clusterName = 'test-cluster';

const mockTopicTableParams: { showInternal?: boolean }[] = [];

jest.mock(
  'components/Topics/List/TopicTable',
  () =>
    ({ params }: { params: { showInternal?: boolean } }) => {
      mockTopicTableParams.push(params);
      return <>TopicTableMock</>;
    }
);

describe('ListPage Component', () => {
  const renderComponent = () =>
    render(
      <ClusterContext.Provider
        value={{
          isReadOnly: false,
          hasKafkaConnectConfigured: true,
          hasSchemaRegistryConfigured: true,
          isTopicDeletionAllowed: true,
          ftsEnabled: false,
          ftsDefaultEnabled: false,
        }}
      >
        <WithRoute path={clusterTopicsPath()}>
          <ListPage />
        </WithRoute>
      </ClusterContext.Provider>,
      { initialEntries: [clusterTopicsPath(clusterName)] }
    );

  afterEach(() => {
    localStorage.clear();
    mockTopicTableParams.length = 0;
  });

  describe('Component Render', () => {
    beforeEach(() => {
      renderComponent();
    });
    it('hides internal topics by default', () => {
      expect(screen.getByLabelText('Show Internal Topics')).not.toBeChecked();
    });

    it('never requests internal topics before the default is applied', () => {
      expect(mockTopicTableParams.length).toBeGreaterThan(0);
      mockTopicTableParams.forEach((params) =>
        expect(params.showInternal).toBe(false)
      );
    });

    it('handles switch of Internal Topics visibility', async () => {
      const switchInput = screen.getByLabelText('Show Internal Topics');
      expect(switchInput).toBeInTheDocument();

      expect(global.localStorage.getItem('hideInternalTopics')).toBeNull();
      await userEvent.click(switchInput);
      expect(switchInput).toBeChecked();
      expect(global.localStorage.getItem('hideInternalTopics')).toBe('false');
      await userEvent.click(switchInput);
      expect(switchInput).not.toBeChecked();
      expect(global.localStorage.getItem('hideInternalTopics')).toBe('true');
    });

    it('renders the TopicsTable', () => {
      expect(screen.getByText('TopicTableMock')).toBeInTheDocument();
    });
  });

  it('shows internal topics when the user chose to show them before', () => {
    localStorage.setItem('hideInternalTopics', 'false');
    renderComponent();
    expect(screen.getByLabelText('Show Internal Topics')).toBeChecked();
  });
});
