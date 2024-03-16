import { PollingMode } from 'generated-sources';

export const ModeOptions = [
  { value: PollingMode.LATEST, label: 'Oldest' },
  { value: PollingMode.EARLIEST, label: 'Newest' },
  { value: PollingMode.TAILING, label: 'Live' },
  { value: PollingMode.FROM_OFFSET, label: 'From offset' },
  { value: PollingMode.TO_OFFSET, label: 'To offset' },
  { value: PollingMode.FROM_TIMESTAMP, label: 'Since time' },
  { value: PollingMode.TO_TIMESTAMP, label: 'To time' },
];

export function convertStrToPollingMode(
  value: string | number
): PollingMode | undefined {
  return PollingMode[value.toString() as keyof typeof PollingMode];
}
