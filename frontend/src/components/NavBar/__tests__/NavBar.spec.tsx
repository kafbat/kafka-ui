import React from 'react';
import { render } from 'lib/testHelpers';
import NavBar from 'components/NavBar/NavBar';
import { screen, within } from '@testing-library/react';

jest.mock('components/Version/Version', () => () => <div>Version</div>);
jest.mock('components/NavBar/UserInfo/UserInfo', () => () => (
  <div>UserInfo</div>
));

describe('NavBar', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: jest.fn().mockImplementation(() => ({
        matches: false,
        addListener: jest.fn(),
      })),
    });

    render(<NavBar onBurgerClick={jest.fn()} />);
  });

  it('correctly renders header', () => {
    const header = screen.getByLabelText('Page Header');
    expect(header).toBeInTheDocument();
    expect(within(header).getByText('kafbat UI')).toBeInTheDocument();
    expect(within(header).getByText('UserInfo')).toBeInTheDocument();
  });
});
