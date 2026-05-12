import React from 'react';
import { screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';
import ErrorPage from 'components/ErrorPage/ErrorPage';
import { getErrorInfoByCode } from 'components/ErrorPage/utils';

jest.mock('components/ErrorPage/utils', () => ({
  getErrorInfoByCode: jest.fn(),
}));

describe('ErrorPage', () => {
  it('renders error page with default text', () => {
    (getErrorInfoByCode as jest.Mock).mockReturnValue({
      title: '404',
      text: 'Page is not found',
      icon: <span data-testid="icon" />,
    });

    render(<ErrorPage />);

    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Page is not found')).toBeInTheDocument();
    expect(screen.getByText('Refresh')).toBeInTheDocument();
  });

  it('renders error page with custom text and button text', () => {
    (getErrorInfoByCode as jest.Mock).mockReturnValue({
      title: '403',
      text: 'Forbidden',
      icon: <span data-testid="icon" />,
    });

    render(
      <ErrorPage status={403} text="access is denied" btnText="Go back" />
    );

    expect(screen.getByText('403')).toBeInTheDocument();
    expect(screen.getByText('access is denied')).toBeInTheDocument();
    expect(screen.getByText('Go back')).toBeInTheDocument();
  });
});
