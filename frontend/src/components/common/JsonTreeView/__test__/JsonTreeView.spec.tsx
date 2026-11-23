import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import JsonTreeView from 'components/common/JsonTreeView/JsonTreeView';

const data = {
  name: 'kafbat',
  version: 42,
  isActive: true,
  owner: null,
  tags: ['ui', 'kafka'],
  nested: {
    value: 'hidden',
  },
};

describe('JsonTreeView', () => {
  it('renders primitive values and object/array brackets for expanded nodes', () => {
    render(<JsonTreeView data={data} defaultExpandDepth={2} />);

    expect(screen.getByText('"kafbat"')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('true')).toBeInTheDocument();
    expect(screen.getByText('null')).toBeInTheDocument();
    expect(screen.getByText('"ui"')).toBeInTheDocument();
    expect(screen.getByText('"hidden"')).toBeInTheDocument();
  });

  it('collapses nodes past the default expand depth', () => {
    render(<JsonTreeView data={data} defaultExpandDepth={1} />);

    // "nested" itself is visible (depth 1) but its contents (depth 2) are not
    expect(screen.getByText('"nested"')).toBeInTheDocument();
    expect(screen.queryByText('"hidden"')).not.toBeInTheDocument();
  });

  it('expands a collapsed node on click', async () => {
    render(<JsonTreeView data={data} defaultExpandDepth={1} />);

    expect(screen.queryByText('"hidden"')).not.toBeInTheDocument();

    await userEvent.click(
      screen.getByRole('button', { name: /expand nested/i })
    );

    expect(screen.getByText('"hidden"')).toBeInTheDocument();
  });

  it('collapses an expanded node back on second click', async () => {
    render(<JsonTreeView data={data} defaultExpandDepth={2} />);

    const toggle = screen.getByRole('button', { name: /collapse nested/i });
    expect(screen.getByText('"hidden"')).toBeInTheDocument();

    await userEvent.click(toggle);

    expect(screen.queryByText('"hidden"')).not.toBeInTheDocument();
  });

  it('renders array items without quoted keys', () => {
    render(<JsonTreeView data={['ui', 'kafka']} defaultExpandDepth={1} />);

    expect(screen.getByText('"ui"')).toBeInTheDocument();
    expect(screen.getByText('"kafka"')).toBeInTheDocument();
  });

  it('renders an empty object without a toggle', () => {
    render(<JsonTreeView data={{}} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
