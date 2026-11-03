import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import TableRefresh from 'components/common/TableRefresh/TableRefresh';

const STORAGE_KEY = 'topics-refresh-rate';
const PREFIXED_KEY = `kafbat-ui-${STORAGE_KEY}`;

describe('TableRefresh', () => {
  beforeEach(() => localStorage.clear());

  it('renders the split control: a refresh button and the interval toggle', () => {
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Auto refresh interval/ })
    ).toBeInTheDocument();
  });

  it('calls onRefresh when the refresh segment is clicked', async () => {
    const onRefresh = jest.fn();
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={onRefresh} />);
    await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));
    expect(onRefresh).toHaveBeenCalledTimes(1);
  });

  it('disables the refresh segment while fetching', () => {
    render(
      <TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} isFetching />
    );
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeDisabled();
  });

  it('shows no interval label when auto refresh is off (default)', () => {
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    expect(screen.queryByText(/^(2s|5s|10s|15s)$/)).not.toBeInTheDocument();
  });

  it('shows the active interval label when a rate is persisted', () => {
    localStorage.setItem(PREFIXED_KEY, '5');
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    expect(screen.getByText('5s')).toBeInTheDocument();
  });

  it('conveys the active interval in the toggle accessible name', () => {
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    expect(
      screen.getByRole('button', { name: /Auto refresh interval: off/i })
    ).toBeInTheDocument();

    localStorage.setItem(PREFIXED_KEY, '5');
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    expect(
      screen.getByRole('button', { name: /Auto refresh interval: every 5s/i })
    ).toBeInTheDocument();
  });

  it('persists the chosen interval when an option is selected', async () => {
    render(<TableRefresh storageKey={STORAGE_KEY} onRefresh={jest.fn()} />);
    await userEvent.click(
      screen.getByRole('button', { name: /Auto refresh interval/ })
    );
    await userEvent.click(screen.getByText('Every 10s'));
    expect(localStorage.getItem(PREFIXED_KEY)).toBe('10');
    expect(screen.getByText('10s')).toBeInTheDocument();
  });
});
