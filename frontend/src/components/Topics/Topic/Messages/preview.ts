export const ROW_PREVIEW_LENGTH = 512;
export const CONTENT_PREVIEW_LENGTH = 64 * 1024;

// Avoid splitting a UTF-16 surrogate pair at a preview boundary.
export const previewEnd = (text: string, end: number) => {
  if (end < text.length && end > 0 && /[\uD800-\uDBFF]/.test(text[end - 1])) {
    return end - 1;
  }
  return end;
};

export const truncatePreview = (
  text: string | undefined,
  limit = ROW_PREVIEW_LENGTH
) => {
  if (!text || text.length <= limit) return text;
  return `${text.slice(0, previewEnd(text, limit - 3))}...`;
};

export const previewHeaders = (
  headers: Record<string, string | undefined> | undefined
) => {
  let text = '{';
  let first = true;
  if (headers) {
    // Serialize bounded pieces only; an individual header can itself be enormous.
    const keys = Object.keys(headers);
    for (let index = 0; index < keys.length; index += 1) {
      const key = keys[index];
      if (headers[key] !== undefined) {
        const value = headers[key] || '';
        const remaining = CONTENT_PREVIEW_LENGTH - text.length;
        const entry = `${first ? '' : ','}${JSON.stringify(
          key.slice(0, remaining)
        )}:${JSON.stringify(value.slice(0, remaining))}`;
        if (
          key.length > remaining ||
          value.length > remaining ||
          entry.length + text.length + 1 > CONTENT_PREVIEW_LENGTH
        ) {
          text += entry;
          return {
            text: text.slice(0, previewEnd(text, CONTENT_PREVIEW_LENGTH)),
            truncated: true,
          };
        }
        text += entry;
        first = false;
      }
    }
  }
  return { text: headers ? `${text}}` : '', truncated: false };
};
