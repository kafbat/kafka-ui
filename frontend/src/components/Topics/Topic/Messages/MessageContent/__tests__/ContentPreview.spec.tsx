import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'lib/testHelpers';
import { theme } from 'theme/theme';
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

  it.each([CONTENT_PREVIEW_LENGTH - 1, CONTENT_PREVIEW_LENGTH])(
    'keeps the normal editor at %i characters',
    (length) => {
      render(
        <ContentPreview
          data={'x'.repeat(length)}
          schemaType="JSON"
          onDownload={jest.fn()}
        />
      );
      expect(screen.getByText('Formatted editor')).toBeInTheDocument();
    }
  );

  it.each([
    'x'.repeat(CONTENT_PREVIEW_LENGTH + 1),
    `{"value":"${'x'.repeat(CONTENT_PREVIEW_LENGTH * 2)}"}`,
    '{malformed'.repeat(CONTENT_PREVIEW_LENGTH),
    '\n'.repeat(CONTENT_PREVIEW_LENGTH * 2),
  ])('does not mount the formatter for oversized content', (data) => {
    render(
      <ContentPreview data={data} schemaType="JSON" onDownload={jest.fn()} />
    );
    expect(EditorViewer).not.toHaveBeenCalled();
    expect(
      screen.getByLabelText('Large content preview').textContent
    ).toHaveLength(CONTENT_PREVIEW_LENGTH);
  });

  it('shows more in bounded chunks and supports returning to the first chunk', async () => {
    const data = `${'a'.repeat(CONTENT_PREVIEW_LENGTH)}${'b'.repeat(CONTENT_PREVIEW_LENGTH)}last`;
    render(
      <ContentPreview data={data} schemaType="JSON" onDownload={jest.fn()} />
    );
    const preview = screen.getByLabelText('Large content preview');
    expect(screen.getByText('Previous chunk')).toBeDisabled();
    await userEvent.click(screen.getByText('Show next chunk'));
    expect(preview).toHaveTextContent('b'.repeat(CONTENT_PREVIEW_LENGTH));
    expect(screen.getByRole('status')).toHaveTextContent(
      'Chunk 2 of 3 visible.'
    );
    await userEvent.click(screen.getByText('Show next chunk'));
    expect(preview).toHaveTextContent('last');
    expect(screen.getByText('Show next chunk')).toBeDisabled();
    await userEvent.click(screen.getByText('Previous chunk'));
    await userEvent.click(screen.getByText('Previous chunk'));
    expect(preview).toHaveTextContent('a'.repeat(CONTENT_PREVIEW_LENGTH));
    expect(EditorViewer).not.toHaveBeenCalled();
  });

  it('keeps Unicode intact across chunk boundaries', async () => {
    const first = 'a'.repeat(CONTENT_PREVIEW_LENGTH - 1);
    render(
      <ContentPreview
        data={`${first}😀end`}
        schemaType="JSON"
        onDownload={jest.fn()}
      />
    );
    const preview = screen.getByLabelText('Large content preview');
    expect(preview).toHaveTextContent(first);
    await userEvent.click(screen.getByText('Show next chunk'));
    expect(preview).toHaveTextContent('😀end');
  });

  it('resets the preview when data changes', async () => {
    const { rerender } = render(
      <ContentPreview
        data={'a'.repeat(CONTENT_PREVIEW_LENGTH * 2)}
        schemaType="JSON"
        onDownload={jest.fn()}
      />
    );
    await userEvent.click(screen.getByText('Show next chunk'));
    rerender(
      <ContentPreview
        data={'b'.repeat(CONTENT_PREVIEW_LENGTH * 2)}
        schemaType="JSON"
        onDownload={jest.fn()}
      />
    );
    expect(screen.getByText('Previous chunk')).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent(
      `Chunk 1 of 2 visible. Preview range: 1–${CONTENT_PREVIEW_LENGTH.toLocaleString(
        'en-US'
      )} of`
    );
  });

  it('shows truncated headers as plain text even if their prefix is small', () => {
    render(
      <ContentPreview
        data='{"large":'
        schemaType="JSON"
        truncated
        onDownload={jest.fn()}
      />
    );
    expect(EditorViewer).not.toHaveBeenCalled();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Headers exceed the preview limit. Download to view them all.'
    );
    expect(screen.queryByText('Show next chunk')).not.toBeInTheDocument();
  });

  it('reuses themed warning hierarchy and dispatches grouped download actions', async () => {
    const onDownload = jest.fn();
    render(
      <ContentPreview
        data={'x'.repeat(CONTENT_PREVIEW_LENGTH + 1)}
        schemaType="JSON"
        onDownload={onDownload}
      />
    );
    const warning = screen.getByRole('status');
    expect(warning).toHaveStyleRule('background', theme.alert.color.warning);
    expect(warning).toHaveStyleRule('padding', '16px');
    expect(screen.getByLabelText('Large content preview')).toHaveStyleRule(
      'color',
      theme.viewer.wrapper.color
    );
    expect(screen.getByLabelText('Large content preview')).toHaveStyleRule(
      'background-color',
      theme.viewer.wrapper.backgroundColor
    );
    const title = screen.getByText('Preview truncated');
    expect(title).toHaveStyleRule('font-weight', '600');
    expect(title).toHaveStyleRule('font-size', '14px');
    const download = screen.getByRole('button', {
      name: 'Download full message',
    });
    expect(download.parentElement).toBe(
      screen.getByRole('button', { name: 'Show next chunk' }).parentElement
    );
    await userEvent.click(download);
    expect(onDownload).toHaveBeenCalledTimes(1);
    expect(
      screen.queryByText(/Loading paused|Message blocked/)
    ).not.toBeInTheDocument();
  });

  it('shows the current chunk and visible preview range', () => {
    render(
      <ContentPreview
        data={'x'.repeat(CONTENT_PREVIEW_LENGTH * 124 + 1)}
        schemaType="JSON"
        onDownload={jest.fn()}
      />
    );
    expect(screen.getByRole('status')).toHaveTextContent(
      `Chunk 1 of 125 visible. Preview range: 1–${CONTENT_PREVIEW_LENGTH.toLocaleString(
        'en-US'
      )} of ${(CONTENT_PREVIEW_LENGTH * 124 + 1).toLocaleString('en-US')}.`
    );
    expect(
      screen.queryByText(/bounded|without formatting/)
    ).not.toBeInTheDocument();
  });
});
