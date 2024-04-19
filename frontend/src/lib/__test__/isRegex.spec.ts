import isRegex from 'lib/isRegex';

describe('isValidRegexp', () => {
  describe('returns true when', () => {
    const table = ['foo/', 'foo+', 'foo[]', 'foo*'];
    test.each(table)('got %s', (str) => {
      expect(isRegex(str)).toBe(true);
    });
  });

  describe('returns false when', () => {
    it('got simple string', () => {
      const expected = isRegex('simple string');
      expect(expected).toBeFalsy();
    });
  });
});
