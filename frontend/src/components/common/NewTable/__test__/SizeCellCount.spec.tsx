import React from 'react';
import { render, WithRoute } from 'lib/testHelpers';
import Table from 'components/common/NewTable';
import SizeCellCount from 'components/common/NewTable/SizeCellCount';
import { TableProvider } from 'components/common/NewTable/Provider';
import { screen } from '@testing-library/dom';
import { ColumnDef } from '@tanstack/react-table';

jest.mock('react-hook-form', () => ({
  useFormContext: () => ({
    register: jest.fn(),
    getValues: jest.fn(),
    setValue: jest.fn(),
  }),
}));

type Datum = { size: number; count?: number };

// The API omits segmentCount or sends null when no metrics source is configured.
// The generated client turns null into undefined, but the cell must cope with both.
const data: Datum[] = [
  { size: 1234, count: 12 },
  { size: 2048, count: 0 },
  { size: 3, count: undefined },
  { size: 50000, count: null as unknown as undefined },
];

const columns: ColumnDef<Datum>[] = [
  {
    header: 'Size',
    accessorKey: 'size',
    cell: SizeCellCount,
  },
];

const renderComponent = () =>
  render(
    <WithRoute path="/*">
      <TableProvider>
        <Table columns={columns} data={data} />
      </TableProvider>
    </WithRoute>,
    { initialEntries: [''] }
  );

// the size and the suffix are separate nodes inside the cell, so match on the whole cell
const cellWithText = (text: string) =>
  screen.getByText(
    (_, element) =>
      element?.tagName === 'TD' && element.textContent?.trim() === text
  );

describe('SizeCellCount', () => {
  it('appends the segment count when it is known', () => {
    renderComponent();
    expect(cellWithText('1 KB, 12 segment(s)')).toBeInTheDocument();
  });

  it('treats zero as a real count', () => {
    renderComponent();
    expect(cellWithText('2 KB, 0 segment(s)')).toBeInTheDocument();
  });

  it('shows only the size when the count is undefined or null', () => {
    renderComponent();
    expect(screen.getByText('3 Bytes')).toBeInTheDocument();
    expect(screen.getByText('49 KB')).toBeInTheDocument();
    expect(screen.queryByText(/null segment/)).not.toBeInTheDocument();
    expect(screen.queryByText(/undefined segment/)).not.toBeInTheDocument();
  });
});
