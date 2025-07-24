import { formatTimestamp } from 'lib/dateTimeHelpers';

describe('dateTimeHelpers', () => {
  describe('formatTimestamp', () => {
    it('should check the empty case', () => {
      expect(formatTimestamp({ timestamp: '' })).toBe('');
    });

    it('should check the invalid case', () => {
      expect(formatTimestamp({ timestamp: 'invalid' })).toBe('');
    });

    it('should output the correct date', () => {
      const date = new Date();
      expect(formatTimestamp({ timestamp: date })).toBe(
        date.toLocaleString([], { hourCycle: 'h23' })
      );
      expect(formatTimestamp({ timestamp: date.getTime() })).toBe(
        date.toLocaleString([], { hourCycle: 'h23' })
      );
    });
  });
});
