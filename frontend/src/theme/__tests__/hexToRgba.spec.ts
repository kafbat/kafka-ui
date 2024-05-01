import { hexToRgba } from 'theme/hexToRgba';

describe('hexToRgba', () => {
  test('converts a 6-digit hex code to an RGBA string', () => {
    expect(hexToRgba('#ff5733', 1)).toBe('rgba(255, 87, 51, 1)');
  });

  test('converts a 3-digit hex code to an RGBA string', () => {
    expect(hexToRgba('#f80', 0.5)).toBe('rgba(255, 136, 0, 0.5)');
  });

  test('throws an error for invalid hex codes', () => {
    expect(() => {
      hexToRgba('invalidColor', 1);
    }).toThrow('Invalid HEX color.');
  });

  test('correctly applies opacity', () => {
    expect(hexToRgba('#ff5733', 0.75)).toBe('rgba(255, 87, 51, 0.75)');
  });

  test('handles hex codes without a hash symbol', () => {
    expect(hexToRgba('ff5733', 0.25)).toBe('rgba(255, 87, 51, 0.25)');
  });
});
