import React from 'react';
import { screen } from '@testing-library/react';
import InputCellEditMode from 'components/Brokers/Broker/Configs/TableComponents/InputCell/InputCellEditMode';
import { render } from 'lib/testHelpers';
import userEvent from '@testing-library/user-event';

describe('InputCellEditMode', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    render(
      <InputCellEditMode
        initialValue="test"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );
  });

  it('renders with initial value', () => {
    expect(screen.getByRole('textbox', { name: /inputValue/i })).toHaveValue(
      'test'
    );
  });

  it('calls onSave with new value', async () => {
    const user = userEvent.setup();
    await user.type(
      screen.getByRole('textbox', { name: /inputValue/i }),
      '123'
    );
    await user.click(screen.getByRole('button', { name: /confirmAction/i }));
    expect(mockOnSave).toHaveBeenCalledWith('test123');
  });

  it('calls onCancel', async () => {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /cancelAction/i }));
    expect(mockOnCancel).toHaveBeenCalled();
  });
});
