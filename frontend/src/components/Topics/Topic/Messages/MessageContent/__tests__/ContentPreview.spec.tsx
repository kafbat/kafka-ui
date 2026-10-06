import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import EditorViewer from 'components/common/EditorViewer/EditorViewer';
import { CONTENT_PREVIEW_LENGTH } from 'components/Topics/Topic/Messages/preview';
import ContentPreview from 'components/Topics/Topic/Messages/MessageContent/ContentPreview';

jest.mock('components/common/EditorViewer/EditorViewer', () => ({
  __esModule: true,
  default: jest.fn(() => <div>Formatted editor</div>),
}));

describe('ContentPreview', () => {
  beforeEach(() => {
    jest
      .mocked(EditorViewer)
      .mockImplementation(() => <div>Formatted editor</div>);
  });

  it('keeps the normal editor at the threshold', () => {
    render(
      <ContentPreview
        data={'x'.repeat(CONTENT_PREVIEW_LENGTH)}
        schemaType="JSON"
      />
    );
    expect(screen.getByText('Formatted editor')).toBeInTheDocument();
  });

  it.each([
    'x'.repeat(CONTENT_PREVIEW_LENGTH + 1),
    `{"value":"${'x'.repeat(CONTENT_PREVIEW_LENGTH * 2)}"}`,
    '{malformed'.repeat(CONTENT_PREVIEW_LENGTH),
    '\n'.repeat(CONTENT_PREVIEW_LENGTH * 2),
  ])('does not mount the formatter for oversized content', (data) => {
    render(<ContentPreview data={data} schemaType="JSON" />);
    expect(EditorViewer).not.toHaveBeenCalled();
    expect(
      screen.getByLabelText('Large content preview').textContent
    ).toHaveLength(CONTENT_PREVIEW_LENGTH);
  });

  it('shows more in bounded chunks and supports returning to the first chunk', async () => {
    const data = `${'a'.repeat(CONTENT_PREVIEW_LENGTH)}${'b'.repeat(CONTENT_PREVIEW_LENGTH)}last`;
    render(<ContentPreview data={data} schemaType="JSON" />);
    const preview = screen.getByLabelText('Large content preview');
    expect(screen.getByText('Previous preview')).toBeDisabled();
    await userEvent.click(screen.getByText('Show more'));
    expect(preview).toHaveTextContent('b'.repeat(CONTENT_PREVIEW_LENGTH));
    await userEvent.click(screen.getByText('Show more'));
    expect(preview).toHaveTextContent('last');
    expect(screen.getByText('Show more')).toBeDisabled();
    await userEvent.click(screen.getByText('Previous preview'));
    await userEvent.click(screen.getByText('Previous preview'));
    expect(preview).toHaveTextContent('a'.repeat(CONTENT_PREVIEW_LENGTH));
    expect(EditorViewer).not.toHaveBeenCalled();
  });

  it('keeps Unicode intact across chunk boundaries', async () => {
    const first = 'a'.repeat(CONTENT_PREVIEW_LENGTH - 1);
    render(<ContentPreview data={`${first}😀end`} schemaType="JSON" />);
    const preview = screen.getByLabelText('Large content preview');
    expect(preview).toHaveTextContent(first);
    await userEvent.click(screen.getByText('Show more'));
    expect(preview).toHaveTextContent('😀end');
  });

  it('resets the preview when data changes', async () => {
    const { rerender } = render(
      <ContentPreview
        data={'a'.repeat(CONTENT_PREVIEW_LENGTH * 2)}
        schemaType="JSON"
      />
    );
    await userEvent.click(screen.getByText('Show more'));
    rerender(
      <ContentPreview
        data={'b'.repeat(CONTENT_PREVIEW_LENGTH * 2)}
        schemaType="JSON"
      />
    );
    expect(screen.getByText('Previous preview')).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent(
      `Showing characters 1-${CONTENT_PREVIEW_LENGTH}`
    );
  });

  it('shows truncated headers as plain text even if their prefix is small', () => {
    render(<ContentPreview data='{"large":' schemaType="JSON" truncated />);
    expect(EditorViewer).not.toHaveBeenCalled();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Headers preview truncated'
    );
    expect(screen.queryByText('Show more')).not.toBeInTheDocument();
  });
});
