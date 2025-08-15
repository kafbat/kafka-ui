import { useLocalStorage } from 'lib/hooks/useLocalStorage';

interface Timezone {
  value: string;
  label: string;
  offset: string;
  UTCOffset: string;
}

const UTCPlain: Timezone = {
  value: 'UTC',
  label: 'Plain UTC',
  offset: 'UTC+00:00',
  UTCOffset: 'UTC+00:00',
};

const generateTimezones = (): Timezone[] => {
  try {
    const timezones = Intl.supportedValuesOf('timeZone').map((timeZone) => {
      try {
        const offsetPart =
          new Intl.DateTimeFormat('en', {
            timeZone,
            timeZoneName: 'shortOffset',
          })
            .formatToParts()
            .find((part) => part.type === 'timeZoneName')?.value || '+00:00';

        let offset: string;

        if (!offsetPart || offsetPart.trim() === '') {
          offset = 'GMT+00:00';
        } else if (offsetPart.startsWith('GMT')) {
          const gmtPart = offsetPart.replace('GMT', '').trim();
          if (!gmtPart) {
            offset = 'GMT+00:00';
          } else if (gmtPart.includes(':')) {
            offset = offsetPart;
          } else {
            const sign =
              gmtPart.startsWith('+') || gmtPart.startsWith('-') ? '' : '+';
            offset = `GMT${sign}${gmtPart}:00`;
          }
        } else if (offsetPart.startsWith('+') || offsetPart.startsWith('-')) {
          if (offsetPart.includes(':')) {
            offset = `GMT${offsetPart}`;
          } else {
            offset = `GMT${offsetPart}:00`;
          }
        } else {
          offset = 'GMT+00:00';
        }

        return {
          value: timeZone,
          label: `${offset.replace('GMT', 'UTC')} ${timeZone.replace(/_/g, ' ')}`,
          offset,
          UTCOffset: offset.replace('GMT', 'UTC'),
        };
      } catch (error) {
        return {
          value: timeZone,
          label: timeZone.replace(/_/g, ' '),
          offset: 'GMT+00:00',
          UTCOffset: 'UTC+00:00',
        };
      }
    });
    timezones.push(UTCPlain);
    return timezones;
  } catch (error) {
    // eslint-disable-next-line no-console
    console.warn(
      'Intl.supportedValuesOf not supported, using fallback timezones'
    );
    const timezones = [
      {
        value: 'UTC',
        label: 'UTC',
        offset: 'GMT+00:00',
        UTCOffset: 'GMT+00:00',
      },
      {
        value: 'America/New York',
        label: 'America/New York',
        offset: 'GMT-05:00',
        UTCOffset: 'GMT-05:00',
      },
      {
        value: 'Europe/London',
        label: 'Europe/London',
        offset: 'GMT+00:00',
        UTCOffset: 'GMT+00:00',
      },
      {
        value: 'Asia/Tokyo',
        label: 'Asia/Tokyo',
        offset: 'GMT+09:00',
        UTCOffset: 'GMT+09:00',
      },
    ];
    timezones.push(UTCPlain);
    return timezones;
  }
};

const TIMEZONES: Timezone[] = generateTimezones().sort((a, b) => {
  const parseOffset = (offset: string): number => {
    const offsetPart = offset.replace('GMT', '');

    const match = offsetPart.match(/([+-])(\d{1,2}):?(\d{0,2})/);
    if (!match) return 0;

    const sign = match[1] === '+' ? 1 : -1;
    const hours = parseInt(match[2], 10);
    const minutes = parseInt(match[3] || '0', 10);

    return sign * (hours * 60 + minutes);
  };

  const offsetA = parseOffset(a.offset);
  const offsetB = parseOffset(b.offset);

  if (offsetA !== offsetB) {
    return offsetA - offsetB;
  }

  return a.value.localeCompare(b.value);
});

export const getSystemTimezone = (): Timezone => {
  const systemTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

  const matchedTimezone = TIMEZONES.find((tz) => tz.value === systemTimezone);

  if (matchedTimezone) {
    return matchedTimezone;
  }

  const now = new Date();
  const offset = -now.getTimezoneOffset() / 60;
  const offsetStr = `GMT${offset >= 0 ? '+' : ''}${offset.toString().padStart(2, '0')}:00`;

  return {
    value: systemTimezone,
    label: systemTimezone,
    offset: offsetStr,
    UTCOffset: offsetStr.replace('GMT', 'UTC'),
  };
};

const TIMEZONE_STORAGE_KEY = `timezone`;

export const useTimezone = () => {
  const [currentTimezone, setCurrentTimezone] =
    useLocalStorage<Timezone | null>(TIMEZONE_STORAGE_KEY, null);

  const setTimezone = (timezone: Timezone | null) => {
    setCurrentTimezone(timezone);
  };

  const getDateInCurrentTimezone = (date: Date = new Date()): Date => {
    const timezone = (currentTimezone ?? getSystemTimezone()).value;

    const timeInTimezone = date.toLocaleString('sv-SE', {
      timeZone: timezone,
    });

    return new Date(timeInTimezone);
  };

  return {
    currentTimezone: currentTimezone ?? getSystemTimezone(),
    availableTimezones: TIMEZONES,
    setTimezone,
    getDateInCurrentTimezone,
  };
};

export type { Timezone };
export { TIMEZONES };
