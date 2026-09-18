import {
  createColumnHelper,
  createTable,
  filterFns,
  getCoreRowModel,
} from '@tanstack/react-table';
import { exportTableCSV } from 'components/common/NewTable/utils/exportTableCSV';

type Row = {
  name: string;
  value: string | number | null | undefined;
};

const columnHelper = createColumnHelper<Row>();

const columns = [
  columnHelper.accessor('name', { header: 'Name' }),
  columnHelper.accessor('value', { header: 'Value' }),
];

const buildTable = (rows: Row[], cols = columns) =>
  createTable<Row>({
    data: rows,
    columns: cols,
    state: { rowSelection: {} },
    getCoreRowModel: getCoreRowModel(),
    enableRowSelection: true,
    onStateChange: () => {},
    renderFallbackValue: null,
    filterFns: {
      ...filterFns,
      includesSome: () => true,
      noop: () => true,
    },
  });

describe('exportTableCSV', () => {
  let capturedBlob: Blob | null = null;
  let capturedFilename: string | null = null;

  beforeEach(() => {
    capturedBlob = null;
    capturedFilename = null;
    global.URL.createObjectURL = jest.fn((blob: Blob) => {
      capturedBlob = blob;
      return 'blob:mock';
    });
    HTMLAnchorElement.prototype.click = jest
      .fn()
      .mockImplementation(function clickMock(this: HTMLAnchorElement) {
        capturedFilename = this.download;
      });
  });

  const readBlob = (blob: Blob) =>
    new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsText(blob);
    });

  const getCsv = async (
    rows: Row[],
    options?: Parameters<typeof exportTableCSV>[1],
    cols = columns
  ) => {
    await exportTableCSV(buildTable(rows, cols), options);
    expect(capturedBlob).not.toBeNull();
    return readBlob(capturedBlob!);
  };

  describe('formula injection protection', () => {
    it.each([
      ['=SUM(A1:A2)', "'=SUM(A1:A2)"],
      ['+cmd', "'+cmd"],
      ['-x', "'-x"],
      ['@x', "'@x"],
      ['\tprefixed', "'\tprefixed"],
    ])('prefixes %p with a single quote', async (input, expected) => {
      const csv = await getCsv([{ name: input, value: 'hello' }]);
      expect(csv).toBe(`Name,Value\n${expected},hello`);
    });

    it('applies both the quote prefix and RFC 4180 quoting', async () => {
      const csv = await getCsv([{ name: '=a,b', value: 'hello' }]);
      expect(csv).toBe(`Name,Value\n"'=a,b",hello`);
    });

    it.each([
      ['\r=1+1', '"\'\r=1+1"'],
      ['\rprefixed', '"\'\rprefixed"'],
      ['abc\r=1+1', '"abc\r=1+1"'],
    ])('quotes %p after formula neutralization', async (input, expected) => {
      const csv = await getCsv([{ name: input, value: 'hello' }]);
      expect(csv).toBe(`Name,Value\n${expected},hello`);
    });
  });

  describe('regression: existing behavior unchanged', () => {
    it.each([
      ['hello', 'hello'],
      ['a,b', '"a,b"'],
      ['a"b', '"a""b"'],
    ])('keeps %p as %p', async (input, expected) => {
      const csv = await getCsv([{ name: input, value: 'hello' }]);
      expect(csv).toBe(`Name,Value\n${expected},hello`);
    });

    it('exports numbers as-is', async () => {
      const csv = await getCsv([{ name: 'n', value: 0 }]);
      expect(csv).toBe('Name,Value\nn,0');
    });

    it('exports null and undefined as empty cells', async () => {
      const csv = await getCsv([
        { name: 'null', value: null },
        { name: 'undef', value: undefined },
      ]);
      expect(csv).toBe('Name,Value\nnull,\nundef,');
    });
  });

  describe('header escaping', () => {
    it('quotes headers containing commas', async () => {
      const cols = [
        columnHelper.accessor('name', { header: 'a,b' }),
        columnHelper.accessor('value', { header: 'Value' }),
      ];
      const csv = await getCsv([{ name: 'x', value: 'y' }], undefined, cols);
      expect(csv).toBe('"a,b",Value\nx,y');
    });

    it('prefixes formula-trigger headers with a single quote', async () => {
      const cols = [
        columnHelper.accessor('name', { header: '=SUM(A1)' }),
        columnHelper.accessor('value', { header: 'Value' }),
      ];
      const csv = await getCsv([{ name: 'x', value: 'y' }], undefined, cols);
      expect(csv).toBe("'=SUM(A1),Value\nx,y");
    });
  });

  it('uses the provided filename', async () => {
    await exportTableCSV(buildTable([{ name: 'a', value: 'b' }]), {
      prefix: 'test',
      includeDate: false,
    });
    expect(capturedFilename).toBe('test.csv');
  });
});
