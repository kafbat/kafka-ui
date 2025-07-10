import React from 'react';
import { screen } from '@testing-library/react';
import ActionPermissionWrapper from 'components/common/ActionComponent/ActionPermissionWrapper/ActionPermissionWrapper';
import { render } from 'lib/testHelpers';
import { Action, ResourceType } from 'generated-sources';
import { usePermission } from 'lib/hooks/usePermission';
import userEvent from '@testing-library/user-event';

jest.mock('lib/hooks/usePermission', () => ({
  usePermission: jest.fn(),
}));

const onActionMock = jest.fn();

const testText = 'test';
const TestComponent = () => <div>{testText}</div>;

describe('ActionPermissionWrapper', () => {
  it('children renders', () => {
    render(
      <ActionPermissionWrapper
        permission={{
          action: Action.CREATE,
          resource: ResourceType.CONNECT,
        }}
        onAction={onActionMock}
      >
        <TestComponent />
      </ActionPermissionWrapper>
    );
    expect(screen.getByText(testText)).toBeInTheDocument();
  });

  it('action calls when allowed', async () => {
    (usePermission as jest.Mock).mockImplementation(() => true);
    render(
      <ActionPermissionWrapper
        permission={{
          action: Action.CREATE,
          resource: ResourceType.CONNECT,
        }}
        onAction={onActionMock}
      >
        <TestComponent />
      </ActionPermissionWrapper>
    );
    await userEvent.click(screen.getByText(testText));
    expect(onActionMock).toHaveBeenCalledTimes(1);
  });

  it('action not calls when not allowed', async () => {
    (usePermission as jest.Mock).mockImplementation(() => false);
    render(
      <ActionPermissionWrapper
        permission={{
          action: Action.CREATE,
          resource: ResourceType.CONNECT,
        }}
        onAction={onActionMock}
      >
        <TestComponent />
      </ActionPermissionWrapper>
    );
    await userEvent.click(screen.getByText(testText));
    expect(onActionMock).not.toHaveBeenCalled();
  });
});
