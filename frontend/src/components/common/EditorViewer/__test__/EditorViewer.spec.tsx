import React from 'react';
import EditorViewer, {
  EditorViewerProps,
} from 'components/common/EditorViewer/EditorViewer';
import { render } from 'lib/testHelpers';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

const data = { a: 1 };
const maxLines = 28;
const schemaType = 'JSON';

describe('EditorViewer component', () => {
  const setupComponent = (props: EditorViewerProps) =>
    render(<EditorViewer {...props} />);

  it('renders JSONTree', () => {
    setupComponent({
      data: JSON.stringify(data),
      maxLines,
      schemaType,
    });
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('to be in the document with fixed height with no value', () => {
    setupComponent({
      data: '',
      maxLines,
      schemaType,
    });
  });

  it('shows a Code/Tree toggle for JSON data, defaulting to Code view', () => {
    setupComponent({
      data: JSON.stringify(data),
      maxLines,
      schemaType,
    });
    expect(screen.getByRole('button', { name: 'Code' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tree' })).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('switches to the tree view when Tree is clicked', async () => {
    setupComponent({
      data: JSON.stringify(data),
      maxLines,
      schemaType,
    });

    await userEvent.click(screen.getByRole('button', { name: 'Tree' }));

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('does not show the toggle for non-JSON/AVRO schema types', () => {
    setupComponent({
      data: 'syntax Foo {}',
      maxLines,
      schemaType: 'PROTOBUF',
    });
    expect(
      screen.queryByRole('button', { name: 'Tree' })
    ).not.toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });
});
