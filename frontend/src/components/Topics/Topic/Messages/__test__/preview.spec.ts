import {
  CONTENT_PREVIEW_LENGTH,
  ROW_PREVIEW_LENGTH,
  previewHeaders,
  truncatePreview,
} from 'components/Topics/Topic/Messages/preview';

describe('message previews', () => {
  it('preserves empty and short values', () => {
    expect(truncatePreview(undefined)).toBeUndefined();
    expect(truncatePreview('')).toBe('');
    expect(truncatePreview('x'.repeat(ROW_PREVIEW_LENGTH))).toHaveLength(
      ROW_PREVIEW_LENGTH
    );
  });

  it('bounds row previews including the truncation indicator', () => {
    const original = 'x'.repeat(ROW_PREVIEW_LENGTH + 1);
    expect(truncatePreview(original)).toBe(
      `${'x'.repeat(ROW_PREVIEW_LENGTH - 3)}...`
    );
    expect(original).toHaveLength(ROW_PREVIEW_LENGTH + 1);
  });

  it.each([ROW_PREVIEW_LENGTH - 1, ROW_PREVIEW_LENGTH, ROW_PREVIEW_LENGTH + 1])(
    'bounds a row of %i code units',
    (length) => {
      const text = 'x'.repeat(length);
      expect(truncatePreview(text)).toHaveLength(
        Math.min(length, ROW_PREVIEW_LENGTH)
      );
      if (length <= ROW_PREVIEW_LENGTH)
        expect(truncatePreview(text)).toBe(text);
    }
  );

  it('does not split surrogate pairs', () => {
    expect(truncatePreview('abc😀rest', 7)).toBe('abc...');
  });

  it('preserves serialization for small headers', () => {
    const headers = { a: 'hello\n"', b: undefined, c: '' };
    expect(previewHeaders(headers)).toEqual({
      text: JSON.stringify(headers),
      truncated: false,
    });
    expect(previewHeaders(undefined)).toEqual({ text: '', truncated: false });
  });

  it.each([
    CONTENT_PREVIEW_LENGTH - 1,
    CONTENT_PREVIEW_LENGTH,
    CONTENT_PREVIEW_LENGTH + 1,
  ])('bounds headers serialized to %i code units', (length) => {
    const headers = {
      a: 'x'.repeat(length - JSON.stringify({ a: '' }).length),
    };
    const preview = previewHeaders(headers);
    expect(preview.text).toHaveLength(Math.min(length, CONTENT_PREVIEW_LENGTH));
    expect(preview.truncated).toBe(length > CONTENT_PREVIEW_LENGTH);
    if (!preview.truncated) expect(preview.text).toBe(JSON.stringify(headers));
  });

  it.each([
    ['large value', () => ({ huge: 'x'.repeat(CONTENT_PREVIEW_LENGTH * 5) })],
    [
      'large name',
      () => ({ ['x'.repeat(CONTENT_PREVIEW_LENGTH * 5)]: 'value' }),
    ],
    ['escaped value', () => ({ escaped: '\n'.repeat(CONTENT_PREVIEW_LENGTH) })],
    [
      'many headers',
      () =>
        Object.fromEntries(
          Array.from({ length: 10000 }, (_, i) => [i.toString(), 'small value'])
        ),
    ],
  ])('bounds oversized header previews: %s', (_name, createHeaders) => {
    const preview = previewHeaders(createHeaders());
    expect(preview.truncated).toBe(true);
    expect(preview.text.length).toBeLessThanOrEqual(CONTENT_PREVIEW_LENGTH);
  });
});
