import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import InputCellViewMode from 'components/Brokers/Broker/Configs/TableComponents/InputCell/InputCellViewMode';

describe('InputCellViewMode', () => {
  const mockOnEdit = jest.fn();
  const value = 'testValue';

  it('displays the correct value for non-sensitive data', () => {
    render(
      <InputCellViewMode
        value={value}
        unit={undefined}
        onEdit={mockOnEdit}
        isDynamic
        isSensitive={false}
        isReadOnly={false}
      />
    );
    expect(screen.getByTitle(value)).toBeInTheDocument();
  });

  it('masks sensitive data with asterisks', () => {
    render(
      <InputCellViewMode
        value={value}
        unit={undefined}
        onEdit={mockOnEdit}
        isDynamic
        isSensitive
        isReadOnly={false}
      />
    );
    expect(screen.getByTitle('Sensitive Value')).toBeInTheDocument();
    expect(screen.getByText('**********')).toBeInTheDocument();
  });

  it('renders edit button and triggers onEdit callback when clicked', async () => {
    const user = userEvent.setup();
    render(
      <InputCellViewMode
        value={value}
        unit={undefined}
        onEdit={mockOnEdit}
        isDynamic
        isSensitive={false}
        isReadOnly={false}
      />
    );
    await user.click(screen.getByLabelText('editAction'));
    expect(mockOnEdit).toHaveBeenCalled();
  });

  it('disables edit button for read-only properties', () => {
    render(
      <InputCellViewMode
        value={value}
        unit={undefined}
        onEdit={mockOnEdit}
        isDynamic
        isSensitive={false}
        isReadOnly
      />
    );
    expect(screen.getByLabelText('editAction')).toBeDisabled();
  });
});
