import Search from 'components/common/Search/Search';
import React from 'react';
import { render } from 'lib/testHelpers';
import userEvent from '@testing-library/user-event';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { useSearchParams } from 'react-router-dom';

jest.mock('use-debounce', () => ({
  useDebouncedCallback: (fn: (e: Event) => void) => fn,
}));

const setSearchParamsMock = jest.fn();
jest.mock('react-router-dom', () => ({
  ...(jest.requireActual('react-router-dom') as object),
  useSearchParams: jest.fn(),
}));

const placeholder = 'I am a search placeholder';
let searchParamsMock: URLSearchParams;

describe('Search', () => {
  beforeEach(() => {
    setSearchParamsMock.mockClear();
    searchParamsMock = new URLSearchParams();
    (useSearchParams as jest.Mock).mockImplementation(() => [
      searchParamsMock,
      setSearchParamsMock,
    ]);
  });
  it('calls handleSearch on input', async () => {
    render(<Search placeholder={placeholder} />);
    const input = screen.getByPlaceholderText(placeholder);
    await userEvent.click(input);
    await userEvent.keyboard('value');
    expect(setSearchParamsMock).toHaveBeenCalledTimes(5);
  });

  it('updates search params from the latest URL state', () => {
    render(<Search placeholder={placeholder} />);

    fireEvent.change(screen.getByPlaceholderText(placeholder), {
      target: { value: 'topic' },
    });

    const updateSearchParams = setSearchParamsMock.mock.calls[0][0] as (
      params: URLSearchParams
    ) => URLSearchParams;
    const nextParams = updateSearchParams(
      new URLSearchParams('page=3&cluster=local')
    );

    expect(nextParams.get('q')).toBe('topic');
    expect(nextParams.get('page')).toBe('1');
    expect(nextParams.get('cluster')).toBe('local');
  });

  it('when placeholder is provided', () => {
    render(<Search placeholder={placeholder} />);
    expect(screen.getByPlaceholderText(placeholder)).toBeInTheDocument();
  });

  it('when placeholder is not provided', () => {
    render(<Search />);
    expect(screen.queryByPlaceholderText('Search')).toBeInTheDocument();
  });

  it('Clear button is not visible by default', async () => {
    render(<Search placeholder={placeholder} />);

    const clearButton = screen.queryByTestId('search-clear-button');
    expect(clearButton).not.toBeInTheDocument();
  });

  it('Clear button is visible if value passed', async () => {
    render(<Search placeholder={placeholder} value="text" />);

    const clearButton = screen.queryByTestId('search-clear-button');
    expect(clearButton).toBeInTheDocument();
  });

  it('Clear button should clear text from input', async () => {
    render(<Search placeholder={placeholder} onChange={jest.fn()} />);

    const searchField = screen.getAllByRole('textbox')[0];
    await userEvent.type(searchField, 'hello');
    expect(searchField).toHaveValue('hello');

    const clearButton = await screen.findByTestId('search-clear-button');
    expect(clearButton).toBeInTheDocument();
    await userEvent.click(clearButton);

    expect(searchField).toHaveValue('');

    await waitFor(() =>
      expect(
        screen.queryByTestId('search-clear-button')
      ).not.toBeInTheDocument()
    );
  });
});
