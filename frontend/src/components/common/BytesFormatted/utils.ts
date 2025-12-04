export const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

export const formatBytes = (
  value: string | number | undefined,
  precision: number = 0
): string => {
  try {
    const bytes = typeof value === 'string' ? parseInt(value, 10) : value;
    if (Number.isNaN(bytes) || (bytes && bytes < 0)) return '-Bytes';
    if (!bytes || bytes < 1024) return `${Math.ceil(bytes || 0)} Bytes`;

    const pow = Math.floor(Math.log2(bytes) / 10);
    const multiplier = 10 ** (precision < 0 ? 0 : precision);

    return `${Math.round((bytes * multiplier) / 1024 ** pow) / multiplier} ${sizes[pow]}`;
  } catch (e) {
    return '-Bytes';
  }
};
