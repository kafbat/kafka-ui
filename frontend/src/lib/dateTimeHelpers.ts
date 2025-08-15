import { getSystemTimezone } from 'lib/hooks/useTimezones';

export const formatTimestamp = ({
  timestamp,
  format = { hourCycle: 'h23' },
  timezone,
  withMilliseconds,
}: {
  timestamp: number | string | Date | undefined;
  format?: Intl.DateTimeFormatOptions;
  timezone?: string;
  withMilliseconds?: boolean;
}): string => {
  if (!timestamp) {
    return '';
  }

  const date = new Date(timestamp);
  // invalid date
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  // browser support
  const language = navigator.language || navigator.languages[0];

  const finalTimezone = timezone || getSystemTimezone().value;

  const formatOptions: Intl.DateTimeFormatOptions = {
    ...format,
    timeZone: finalTimezone,
  };

  let formattedTimestamp = date.toLocaleString(language || [], formatOptions);

  if (withMilliseconds) {
    formattedTimestamp += `.${date.getMilliseconds()}`;
  }

  return formattedTimestamp;
};

export const formatMilliseconds = (input = 0) => {
  const milliseconds = Math.max(input || 0, 0);

  const seconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  }

  if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  }

  if (seconds > 0) {
    return `${seconds}s`;
  }

  return `${milliseconds}ms`;
};

export const passedTime = (value: number) => (value < 10 ? `0${value}` : value);

export const calculateTimer = (startedAt: number) => {
  const now = new Date().getTime();
  const elapsedMillis = now - startedAt;

  if (elapsedMillis < 0) {
    return '00:00';
  }

  const seconds = Math.floor(elapsedMillis / 1000) % 60;
  const minutes = Math.floor(elapsedMillis / 60000);

  return `${passedTime(minutes)}:${passedTime(seconds)}`;
};
