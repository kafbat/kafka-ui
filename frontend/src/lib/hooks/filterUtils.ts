import { PollingMode } from 'generated-sources';

export const ModeOptions = [
  { value: PollingMode.LATEST, label: 'Newest' },
  { value: PollingMode.EARLIEST, label: 'Oldest' },
  { value: PollingMode.TAILING, label: 'Live' },
  { value: PollingMode.FROM_OFFSET, label: 'From offset' },
  { value: PollingMode.TO_OFFSET, label: 'To offset' },
  { value: PollingMode.FROM_TIMESTAMP, label: 'Since time' },
  { value: PollingMode.TO_TIMESTAMP, label: 'To time' },
  { value: PollingMode.TIMESTAMP_RANGE, label: 'Time range' },
];

export function convertStrToPollingMode(
  value: string | number
): PollingMode | undefined {
  return PollingMode[value.toString() as keyof typeof PollingMode];
}
