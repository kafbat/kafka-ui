import React from 'react';
import DurationFormatted from 'components/common/DurationFormatted/DurationFormatted';
import { render, screen } from '@testing-library/react';
import { formatDuration } from 'components/common/DurationFormatted/utils';

describe('formatDuration', () => {
  it('formats years correctly', () => {
    // 1 year = 365 * 24 * 60 * 60 * 1000 = 31536000000 ms
    expect(formatDuration(31536000000)).toBe('1 year');
    expect(formatDuration(63072000000)).toBe('2 years');
  });

  it('formats weeks correctly', () => {
    // 1 week = 7 * 24 * 60 * 60 * 1000 = 604800000 ms
    expect(formatDuration(604800000)).toBe('1 week');
    // 4 weeks = 2419200000 ms (common retention.ms value)
    expect(formatDuration(2419200000)).toBe('4 weeks');
  });

  it('formats days correctly', () => {
    // 1 day = 24 * 60 * 60 * 1000 = 86400000 ms
    expect(formatDuration(86400000)).toBe('1 day');
    expect(formatDuration(172800000)).toBe('2 days');
  });

  it('formats hours correctly', () => {
    // 1 hour = 60 * 60 * 1000 = 3600000 ms
    expect(formatDuration(3600000)).toBe('1 hour');
    expect(formatDuration(7200000)).toBe('2 hours');
  });

  it('formats minutes correctly', () => {
    // 1 minute = 60 * 1000 = 60000 ms
    expect(formatDuration(60000)).toBe('1 minute');
    expect(formatDuration(120000)).toBe('2 minutes');
  });

  it('formats seconds correctly', () => {
    // 1 second = 1000 ms
    expect(formatDuration(1000)).toBe('1 second');
    expect(formatDuration(5000)).toBe('5 seconds');
  });

  it('formats milliseconds correctly', () => {
    expect(formatDuration(500)).toBe('500 ms');
    expect(formatDuration(1)).toBe('1 ms');
  });

  it('handles zero', () => {
    expect(formatDuration(0)).toBe('0 ms');
  });

  it('handles negative values (infinite/unbounded)', () => {
    expect(formatDuration(-1)).toBe('-1');
    expect(formatDuration(-2)).toBe('-2');
  });

  it('handles undefined and null', () => {
    expect(formatDuration(undefined)).toBe('0 ms');
  });

  it('handles string values', () => {
    expect(formatDuration('2419200000')).toBe('4 weeks');
    expect(formatDuration('3600000')).toBe('1 hour');
    expect(formatDuration('-1')).toBe('-1');
  });

  it('handles invalid string values', () => {
    expect(formatDuration('invalid')).toBe('0 ms');
  });

  it('formats combined units for non-exact values', () => {
    // 1 day + 12 hours = 86400000 + 43200000 = 129600000
    expect(formatDuration(129600000)).toBe('1 day 12 hours');
    // 2 weeks + 3 days
    expect(formatDuration(1468800000)).toBe('2 weeks 3 days');
  });
});

describe('DurationFormatted', () => {
  it('renders formatted duration correctly', () => {
    render(<DurationFormatted value={2419200000} />);
    expect(screen.getByText('4 weeks')).toBeInTheDocument();
  });

  it('renders hours correctly', () => {
    render(<DurationFormatted value={3600000} />);
    expect(screen.getByText('1 hour')).toBeInTheDocument();
  });

  it('renders zero correctly', () => {
    render(<DurationFormatted value={0} />);
    expect(screen.getByText('0 ms')).toBeInTheDocument();
  });

  it('renders negative values correctly', () => {
    render(<DurationFormatted value={-1} />);
    expect(screen.getByText('-1')).toBeInTheDocument();
  });

  it('renders undefined correctly', () => {
    render(<DurationFormatted value={undefined} />);
    expect(screen.getByText('0 ms')).toBeInTheDocument();
  });
});
