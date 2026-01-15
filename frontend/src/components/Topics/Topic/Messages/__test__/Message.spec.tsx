import React from 'react';
import { ResourceType } from 'generated-sources';
import Message, { Props } from 'components/Topics/Topic/Messages/Message';
import { act, screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import userEvent from '@testing-library/user-event';
import useAppParams from 'lib/hooks/useAppParams';
import { TopicActionsProvider } from 'components/contexts/TopicActionsContext';
import { timeAgo } from 'lib/dateTimeHelpers';
import { getDefaultActionMessage } from 'components/common/ActionComponent/ActionComponent';
import { UserInfoRolesAccessContext } from 'components/contexts/UserInfoRolesAccessContext';
import { RolesType } from 'lib/permissions';

import {
  mockMessageValue,
  mockNoRoles,
  mockMessageKey,
  mockMessageContentText,
  mockContentFilters,
  mockKeyFilters,
  mockMessage,
  mockRoles,
} from './Message.fixtures';

jest.mock(
  'components/Topics/Topic/Messages/MessageContent/MessageContent',
  () => () => (
    <tr>
      <td>{mockMessageContentText}</td>
    </tr>
  )
);
jest.mock('lib/hooks/useAppParams');

const mockUseAppParams = jest.mocked(useAppParams);
const mockOpenSidebarWithMessage = jest.fn();
const renderComponent = (
  props: Partial<Props> = {
    message: mockMessage,
    keyFilters: [],
    contentFilters: [],
  },
  roles: Map<string, Map<ResourceType, RolesType>> = mockNoRoles
) =>
  render(
    <UserInfoRolesAccessContext.Provider
      value={{
        roles,
        rbacFlag: true,
        username: 'testUsername',
      }}
    >
      <TopicActionsProvider openSidebarWithMessage={mockOpenSidebarWithMessage}>
        <table>
          <tbody>
            <Message
              message={props.message || mockMessage}
              keyFilters={props.keyFilters || []}
              contentFilters={props.contentFilters || []}
            />
          </tbody>
        </table>
      </TopicActionsProvider>
    </UserInfoRolesAccessContext.Provider>
  );

describe('Message component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAppParams.mockReturnValue({
      clusterName: 'test-cluster',
      topicName: 'testTopic',
    });
  });

  it('shows the data in the table row', () => {
    renderComponent();
    expect(screen.getByText(mockMessage.value as string)).toBeInTheDocument();
    expect(screen.getByText(mockMessage.key as string)).toBeInTheDocument();
    expect(
      screen.getByText(timeAgo(mockMessage.timestamp))
    ).toBeInTheDocument();
    expect(screen.getByText(mockMessage.offset.toString())).toBeInTheDocument();
    expect(
      screen.getByText(mockMessage.partition.toString())
    ).toBeInTheDocument();
  });

  it('check the useDataSaver functionality', () => {
    const props = { message: { ...mockMessage } };
    delete props.message.value;
    renderComponent(props);
    expect(
      screen.queryByText(mockMessage.value as string)
    ).not.toBeInTheDocument();
  });

  it('should toggle action dropdown button visibility on hover', async () => {
    renderComponent();
    const trElement = screen.getByRole('row');

    expect(
      screen.queryByRole('button', {
        name: 'Dropdown Toggle',
      })
    ).not.toBeInTheDocument();

    await act(() => userEvent.hover(trElement));

    expect(
      screen.getByRole('button', {
        name: 'Dropdown Toggle',
      })
    ).toBeVisible();

    await act(() => userEvent.unhover(trElement));
    expect(
      screen.queryByRole('button', {
        name: 'Dropdown Toggle',
      })
    ).not.toBeInTheDocument();
  });

  it('should check open Message Content functionality', async () => {
    renderComponent();
    const messageToggleIcon = screen.getByRole('button', { hidden: true });
    expect(screen.queryByText(mockMessageContentText)).not.toBeInTheDocument();
    await act(() => userEvent.click(messageToggleIcon));
    expect(screen.getByText(mockMessageContentText)).toBeInTheDocument();
  });

  it('should check if Preview filter showing for key', () => {
    const props = {
      message: { ...mockMessage, key: mockMessageKey as string },
      keyFilters: [mockKeyFilters],
    };
    renderComponent(props);
    const keyFiltered = screen.getByText('sub: "learnprogramming"');
    expect(keyFiltered).toBeInTheDocument();
  });

  it('should check if Preview filter showing for Value', () => {
    const props = {
      message: { ...mockMessage, value: mockMessageValue as string },
      contentFilters: [mockContentFilters],
    };
    renderComponent(props);
    const keyFiltered = screen.getByText('author: "DwaywelayTOP"');
    expect(keyFiltered).toBeInTheDocument();
  });

  it('shows action options in dropdown on click', async () => {
    renderComponent();
    const trElement = screen.getByRole('row');

    await act(() => userEvent.hover(trElement));

    const dropdownToggle = screen.getByRole('button', {
      name: 'Dropdown Toggle',
    });
    await act(() => userEvent.click(dropdownToggle));

    expect(
      await screen.findByRole('menuitem', { name: 'Copy to clipboard' })
    ).toBeVisible();
    expect(
      await screen.findByRole('menuitem', { name: 'Save as a file' })
    ).toBeVisible();
    expect(
      await screen.findByRole('menuitem', { name: 'Reproduce message' })
    ).toBeVisible();
  });

  it('calls openSidebarWithMessage when "Produce Message" is clicked', async () => {
    renderComponent(undefined, mockRoles);
    const trElement = screen.getByRole('row');
    await act(() => userEvent.hover(trElement));

    const dropdownToggle = screen.getByRole('button', {
      name: 'Dropdown Toggle',
    });
    await act(() => userEvent.click(dropdownToggle));

    const produceMessageButton = screen.getByRole('menuitem', {
      name: 'Reproduce message',
    });
    await act(() => userEvent.click(produceMessageButton));

    expect(mockOpenSidebarWithMessage).toHaveBeenCalledTimes(1);
    expect(mockOpenSidebarWithMessage).toHaveBeenCalledWith({
      timestamp: mockMessage.timestamp,
      timestampType: mockMessage.timestampType,
      offset: mockMessage.offset,
      key: mockMessage.key,
      partition: mockMessage.partition,
      value: mockMessage.value,
      headers: mockMessage.headers,
      valueSerde: mockMessage.valueSerde,
      keySerde: mockMessage.keySerde,
    });
  });

  test.each([
    ['has produce message roles', true],
    ['lacks produce message roles', false],
  ])(
    'when user %s for topic, "Reproduce message" button should be enabled: %s',
    async (_scenario, hasRoles) => {
      renderComponent(undefined, hasRoles ? mockRoles : mockNoRoles);

      const trElement = screen.getByRole('row');
      await act(() => userEvent.hover(trElement));

      const dropdownToggle = screen.getByRole('button', {
        name: 'Dropdown Toggle',
      });
      await act(() => userEvent.click(dropdownToggle));

      const produceMessageButton = screen.getByRole('menuitem', {
        name: 'Reproduce message',
      });
      await act(() => userEvent.hover(produceMessageButton));

      if (hasRoles) {
        expect(produceMessageButton).not.toHaveAttribute(
          'aria-disabled',
          'true'
        );
      } else {
        expect(produceMessageButton).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByText(getDefaultActionMessage())).toBeVisible();
      }
    }
  );
});
