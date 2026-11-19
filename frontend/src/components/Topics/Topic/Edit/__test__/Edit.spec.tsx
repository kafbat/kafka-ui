import React from 'react';
import Edit from 'components/Topics/Topic/Edit/Edit';
import { cleanup, screen } from '@testing-library/react';
import { render, WithRoute } from 'lib/testHelpers';
import userEvent from '@testing-library/user-event';
import { clusterTopicEditPath } from 'lib/paths';
import {
  useTopicConfig,
  useTopicDetails,
  useUpdateTopic,
} from 'lib/hooks/api/topics';
import { internalTopicPayload, topicConfigPayload } from 'lib/fixtures/topics';

const clusterName = 'testCluster';
const topicName = 'testTopic';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('components/Topics/Topic/Edit/DangerZone/DangerZone', () => () => (
  <>DangerZone</>
));

jest.mock('lib/hooks/api/topics', () => ({
  useTopicDetails: jest.fn(),
  useTopicConfig: jest.fn(),
  useUpdateTopic: jest.fn(),
}));

const updateTopicMock = jest.fn();

const renderComponent = (config = topicConfigPayload) => {
  (useTopicConfig as jest.Mock).mockImplementation(() => ({ data: config }));
  const path = clusterTopicEditPath(clusterName, topicName);
  return render(
    <WithRoute path={clusterTopicEditPath()}>
      <Edit />
    </WithRoute>,
    { initialEntries: [path] }
  );
};

describe('Edit Component', () => {
  beforeEach(() => {
    (useTopicDetails as jest.Mock).mockImplementation(() => ({
      data: internalTopicPayload,
    }));
    (useTopicConfig as jest.Mock).mockImplementation(() => ({
      data: topicConfigPayload,
    }));
    (useUpdateTopic as jest.Mock).mockImplementation(() => ({
      isLoading: false,
      mutateAsync: updateTopicMock,
    }));
    renderComponent();
  });

  it('renders DangerZone component', () => {
    expect(screen.getByText(`DangerZone`)).toBeInTheDocument();
  });

  it('submits form correctly', async () => {
    renderComponent();
    const btn = screen.getAllByText(/Update topic/i)[0];
    const field = screen.getByRole('spinbutton', {
      name: 'Min In Sync Replicas Min In Sync Replicas',
    });
    await userEvent.type(field, '1');
    await userEvent.click(btn);
    expect(updateTopicMock).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith('../');
  });

  it('allows overriding a default config with a topic custom parameter', async () => {
    await userEvent.click(screen.getByText('Add Custom Parameter'));
    const parameterInput = screen.getByRole('listitem');
    await userEvent.click(parameterInput);
    await userEvent.click(screen.getByText('message.downconversion.enable'));

    expect(parameterInput).toHaveValue('message.downconversion.enable');
  });

  it('disables read-only config parameters', async () => {
    const config = topicConfigPayload.map((parameter) =>
      parameter.name === 'message.downconversion.enable'
        ? { ...parameter, isReadOnly: true }
        : parameter
    );
    cleanup();
    renderComponent(config);

    await userEvent.click(screen.getByText('Add Custom Parameter'));
    await userEvent.click(screen.getByRole('listitem'));

    const readOnlyOption = screen.getByText('message.downconversion.enable');
    expect(readOnlyOption).toHaveStyleRule('cursor', 'not-allowed');
    await userEvent.click(readOnlyOption);
    expect(screen.getByRole('listitem')).toHaveValue('');
  });
});
