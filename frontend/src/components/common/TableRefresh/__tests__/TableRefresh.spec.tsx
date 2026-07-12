import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import TableRefresh from 'components/common/TableRefresh/TableRefresh';

describe('TableRefresh', () => {
  it('renders a refresh button and the rate select', () => {
    render(
      <TableRefresh storageKey="topics-refresh-rate" onRefresh={jest.fn()} />
    );
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
    expect(screen.getByText(/Refresh rate:/i)).toBeInTheDocument();
  });

  it('calls onRefresh when the button is clicked', async () => {
    const onRefresh = jest.fn();
    render(
      <TableRefresh storageKey="topics-refresh-rate" onRefresh={onRefresh} />
    );
    await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));
    expect(onRefresh).toHaveBeenCalledTimes(1);
  });

  it('disables the button while fetching', () => {
    render(
      <TableRefresh
        storageKey="topics-refresh-rate"
        onRefresh={jest.fn()}
        isFetching
      />
    );
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeDisabled();
  });
});
