const MILLISECONDS_PER_SECOND = 1000;
const MILLISECONDS_PER_MINUTE = 60 * MILLISECONDS_PER_SECOND;
const MILLISECONDS_PER_HOUR = 60 * MILLISECONDS_PER_MINUTE;
const MILLISECONDS_PER_DAY = 24 * MILLISECONDS_PER_HOUR;
const MILLISECONDS_PER_WEEK = 7 * MILLISECONDS_PER_DAY;
const MILLISECONDS_PER_YEAR = 365 * MILLISECONDS_PER_DAY;

export const timeUnits = [
  { unit: 'year', ms: MILLISECONDS_PER_YEAR },
  { unit: 'week', ms: MILLISECONDS_PER_WEEK },
  { unit: 'day', ms: MILLISECONDS_PER_DAY },
  { unit: 'hour', ms: MILLISECONDS_PER_HOUR },
  { unit: 'minute', ms: MILLISECONDS_PER_MINUTE },
  { unit: 'second', ms: MILLISECONDS_PER_SECOND },
] as const;

/**
 * Formats milliseconds into a human-readable duration string.
 * Examples:
 *   - 2419200000 -> "4 weeks"
 *   - 86400000 -> "1 day"
 *   - 3600000 -> "1 hour"
 *   - 60000 -> "1 minute"
 *   - 1000 -> "1 second"
 *   - 500 -> "500 ms"
 *   - 0 -> "0 ms"
 *   - -1 -> "-1" (negative values returned as-is, commonly used for "infinite/unbounded")
 */
export const formatDuration = (value: string | number | undefined): string => {
  try {
    const ms = typeof value === 'string' ? parseInt(value, 10) : value;

    if (ms === undefined || ms === null || Number.isNaN(ms)) {
      return '0 ms';
    }

    // Negative values typically mean "infinite" or "unbounded" in Kafka configs
    if (ms < 0) {
      return String(ms);
    }

    if (ms === 0) {
      return '0 ms';
    }

    // Find the largest unit that fits
    const matchedUnitIndex = timeUnits.findIndex(
      ({ ms: unitMs }) => ms >= unitMs
    );

    if (matchedUnitIndex === -1) {
      // Less than 1 second - show in milliseconds
      return `${ms} ms`;
    }

    const { unit, ms: unitMs } = timeUnits[matchedUnitIndex];
    const count = Math.floor(ms / unitMs);
    const remainder = ms % unitMs;

    // If there's a significant remainder, show two units for better precision
    if (remainder > 0 && matchedUnitIndex + 1 < timeUnits.length) {
      const nextUnit = timeUnits[matchedUnitIndex + 1];
      const nextCount = Math.floor(remainder / nextUnit.ms);
      if (nextCount > 0) {
        return `${count} ${unit}${count !== 1 ? 's' : ''} ${nextCount} ${nextUnit.unit}${nextCount !== 1 ? 's' : ''}`;
      }
    }

    return `${count} ${unit}${count !== 1 ? 's' : ''}`;
  } catch (e) {
    return '0 ms';
  }
};
