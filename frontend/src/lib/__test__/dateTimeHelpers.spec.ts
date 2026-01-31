import {
  passedTime,
  calculateTimer,
  formatMilliseconds,
  timeAgo,
} from 'lib/dateTimeHelpers';

const startedAt = 1664891890889;

describe('format Milliseconds', () => {
  it('hours > 0', () => {
    const result = formatMilliseconds(10000000);

    expect(result).toEqual('2h 46m');
  });
  it('minutes > 0', () => {
    const result = formatMilliseconds(1000000);

    expect(result).toEqual('16m 40s');
  });

  it('seconds > 0', () => {
    const result = formatMilliseconds(10000);

    expect(result).toEqual('10s');
  });

  it('milliseconds > 0', () => {
    const result = formatMilliseconds(100);

    expect(result).toEqual('100ms' || '0ms');
    expect(formatMilliseconds()).toEqual('0ms');
  });
});

describe('calculate timer', () => {
  it('time value < 10', () => {
    expect(passedTime(5)).toBeTruthy();
  });

  it('time value > 9', () => {
    expect(passedTime(10)).toBeTruthy();
  });

  it('run calculate time', () => {
    expect(calculateTimer(startedAt));
  });

  it('return when startedAt > new Date()', () => {
    expect(calculateTimer(1664891890889199)).toBe('00:00');
  });
});

describe('timeAgo', () => {
  const language = navigator.language || navigator.languages[0];
  const rtf = new Intl.RelativeTimeFormat(language, { numeric: 'auto' });

  it('returns empty string for undefined timestamp', () => {
    expect(timeAgo(undefined)).toBe('');
  });

  it('returns empty string for invalid date', () => {
    expect(timeAgo('invalid-date')).toBe('');
  });

  it('returns "now" for timestamps within 1 second', () => {
    const now = new Date();
    expect(timeAgo(now)).toBe(rtf.format(0, 'second'));
  });

  it('returns seconds ago for timestamps within a minute', () => {
    const thirtySecondsAgo = new Date(Date.now() - 30 * 1000);
    expect(timeAgo(thirtySecondsAgo)).toBe(rtf.format(-30, 'second'));
  });

  it('returns "1 minute ago" for timestamps around 1 minute old', () => {
    const oneMinuteAgo = new Date(Date.now() - 60 * 1000);
    expect(timeAgo(oneMinuteAgo)).toBe(rtf.format(-1, 'minute'));
  });

  it('returns minutes ago for timestamps within an hour', () => {
    const twentyMinutesAgo = new Date(Date.now() - 20 * 60 * 1000);
    expect(timeAgo(twentyMinutesAgo)).toBe(rtf.format(-20, 'minute'));
  });

  it('returns "1 hour ago" for timestamps around 1 hour old', () => {
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
    expect(timeAgo(oneHourAgo)).toBe(rtf.format(-1, 'hour'));
  });

  it('returns hours ago for timestamps within a day', () => {
    const threeHoursAgo = new Date(Date.now() - 3 * 60 * 60 * 1000);
    expect(timeAgo(threeHoursAgo)).toBe(rtf.format(-3, 'hour'));
  });

  it('returns "yesterday" for timestamps around 1 day old', () => {
    const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
    expect(timeAgo(oneDayAgo)).toBe(rtf.format(-1, 'day'));
  });

  it('returns days ago for timestamps within 30 days', () => {
    const fiveDaysAgo = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000);
    expect(timeAgo(fiveDaysAgo)).toBe(rtf.format(-5, 'day'));
  });

  it('returns formatted date for timestamps older than 30 days', () => {
    const sixtyDaysAgo = new Date(Date.now() - 60 * 24 * 60 * 60 * 1000);
    const result = timeAgo(sixtyDaysAgo);
    // Should return a locale date string, not "X days ago"
    expect(result).not.toContain('days ago');
  });
});
