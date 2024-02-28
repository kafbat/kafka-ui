import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InputCell, {
  type InputCellProps,
} from 'components/Brokers/Broker/Configs/TableComponents/InputCell/index';
import { render } from 'lib/testHelpers';
import { ConfigSource } from 'generated-sources';
import { useConfirm } from 'lib/hooks/useConfirm';
import { BrokerConfigsTableRow } from 'components/Brokers/Broker/Configs/lib/types';
import { Row } from '@tanstack/react-table';

jest.mock('lib/hooks/useConfirm', () => ({
  useConfirm: jest.fn(),
}));

describe('InputCell', () => {
  const mockOnUpdate = jest.fn();
  const initialValue = 'initialValue';
  const name = 'testName';
  const original = {
    name,
    source: ConfigSource.DYNAMIC_BROKER_CONFIG,
    value: initialValue,
    isSensitive: false,
    isReadOnly: false,
  };

  beforeEach(() => {
    const setupWrapper = (props?: Partial<InputCellProps>) => (
      <InputCell
        {...(props as InputCellProps)}
        row={{ original } as Row<BrokerConfigsTableRow>}
        onUpdate={mockOnUpdate}
      />
    );
    (useConfirm as jest.Mock).mockImplementation(
      () => (message: string, callback: () => void) => callback()
    );
    render(setupWrapper());
  });

  it('renders InputCellViewMode by default', () => {
    expect(screen.getByText(initialValue)).toBeInTheDocument();
  });

  it('switches to InputCellEditMode upon triggering an edit action', async () => {
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('editAction'));
    expect(
      screen.getByRole('textbox', { name: /inputValue/i })
    ).toBeInTheDocument();
  });

  it('calls onUpdate callback with the new value when saved', async () => {
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('editAction')); // Enter edit mode
    await user.type(
      screen.getByRole('textbox', { name: /inputValue/i }),
      '123'
    );
    await user.click(screen.getByRole('button', { name: /confirmAction/i }));
    expect(mockOnUpdate).toHaveBeenCalledWith(name, 'initialValue123');
  });

  it('returns to InputCellViewMode upon canceling an edit', async () => {
    const user = userEvent.setup();
    await user.click(screen.getByLabelText('editAction')); // Enter edit mode
    await user.click(screen.getByRole('button', { name: /cancelAction/i }));
    expect(screen.getByText(initialValue)).toBeInTheDocument(); // Back to view mode
  });
});
