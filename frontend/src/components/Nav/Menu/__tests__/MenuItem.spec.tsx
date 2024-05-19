import React from 'react';
import MenuItem, { MenuItemProps } from 'components/Nav/Menu/MenuItem';
import { screen } from '@testing-library/react';
import { render } from 'lib/testHelpers';

describe('MenuItem', () => {
  const setupComponent = (props: Partial<MenuItemProps> = {}) => (
    <ul>
      <MenuItem to="/test" title="Test title" {...props} />
    </ul>
  );

  const getMenuItem = () => screen.getByRole('menuitem');
  const getLink = () => screen.queryByRole('link');

  it('renders component with correct title', () => {
    const testTitle = 'My Test Title';
    render(setupComponent({ title: testTitle }));
    expect(screen.getByText(testTitle)).toBeInTheDocument();
  });

  it('renders primary variant component with correct styles', () => {
    render(setupComponent({ variant: 'primary' }));
    expect(getMenuItem()).toHaveStyle({ fontWeight: '500' });
  });

  it('renders secondary variant component with correct styles', () => {
    render(setupComponent({ variant: 'secondary' }));
    expect(getMenuItem()).toHaveStyle({ fontWeight: '400' });
  });

  it('renders list item with link inside', () => {
    render(setupComponent({ to: '/my-cluster' }));
    expect(getMenuItem()).toBeInTheDocument();
    expect(getLink()).toBeInTheDocument();
  });
});
