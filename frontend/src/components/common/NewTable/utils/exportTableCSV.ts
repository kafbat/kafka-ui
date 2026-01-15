import type { RowData, Table } from '@tanstack/react-table';
import innerText from 'react-innertext';

const escapeCsv = (value: string) => {
  if (value.includes(',') || value.includes('"') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
};

export type ExportCsvOptions = {
  filename?: string;
  prefix?: string;
  includeDate?: boolean;
  dateFormat?: (d: Date) => string;
};

export const exportTableCSV = <T extends RowData>(
  table: Table<T> | null | undefined,
  options: ExportCsvOptions = {}
) => {
  if (!table) return;

  const {
    filename,
    prefix = 'table_data',
    includeDate = true,
    dateFormat = (d: Date) => d.toISOString().slice(0, 10),
  } = options;

  const selected = table.getSelectedRowModel().rows;
  const paginated = table.getRowModel().rows;
  const all = table.getPrePaginationRowModel().rows;

  // eslint-disable-next-line no-nested-ternary
  const rowsToExport = selected.length
    ? selected
    : all.length
      ? all
      : paginated;

  if (!rowsToExport.length) return;

  const headersColumns = table.getAllColumns().filter((col) => {
    const header = col.columnDef.meta?.csv ?? col.columnDef.header;
    const hasAccessorKey =
      'accessorKey' in col.columnDef && col.columnDef.accessorKey;
    return header && hasAccessorKey;
  });

  const headers = headersColumns.map(
    (col) => col.columnDef.meta?.csv ?? col.columnDef.header ?? col.id
  );

  const body = rowsToExport.map((row) =>
    headersColumns.map((col) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const original = row.original as Record<string, any>;

      if (col.columnDef.meta?.csvFn) {
        return escapeCsv(String(col.columnDef.meta.csvFn(row.original)));
      }

      if (col.columnDef.cell && typeof col.columnDef.cell === 'function') {
        try {
          const accessorKey =
            'accessorKey' in col.columnDef
              ? (col.columnDef.accessorKey as string)
              : undefined;

          const cellValue = col.columnDef.cell({
            getValue: () => (accessorKey ? original[accessorKey] : undefined),
            row,
            column: col,
            table,
            cell: row.getAllCells().find((c) => c.column.id === col.id)!,
            renderValue: () =>
              accessorKey ? original[accessorKey] : undefined,
          });

          if (
            cellValue &&
            typeof cellValue === 'object' &&
            'props' in cellValue
          ) {
            return escapeCsv(innerText(cellValue) || '');
          }

          return escapeCsv(String(cellValue ?? ''));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.warn('CSV export: cell renderer failed', error);
        }
      }

      if ('accessorKey' in col.columnDef && col.columnDef.accessorKey) {
        const accessorKey = col.columnDef.accessorKey as string;
        const value = original[accessorKey];
        if (value === null || value === undefined) {
          return '';
        }

        if (typeof value === 'object') {
          return escapeCsv(JSON.stringify(value));
        }
        return escapeCsv(String(value));
      }

      return '';
    })
  );

  const csv = [headers.join(','), ...body.map((r) => r.join(','))].join('\n');

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const datePart = includeDate ? `_${dateFormat(new Date())}` : '';
  const file = filename ?? `${prefix}${datePart}.csv`;

  const link = document.createElement('a');
  link.href = window.URL.createObjectURL(blob);
  link.setAttribute('download', file);
  document.body.appendChild(link);
  link.click();
  link.remove();
};
