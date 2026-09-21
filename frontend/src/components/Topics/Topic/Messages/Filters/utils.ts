import { PollingMode } from 'generated-sources';
import { Option } from 'react-multi-select-component';

export function isModeOptionWithInput(value: PollingMode) {
  return (
    value !== PollingMode.TAILING &&
    value !== PollingMode.LATEST &&
    value !== PollingMode.EARLIEST
  );
}

export function isModeOffsetSelector(value: PollingMode) {
  return value === PollingMode.TO_OFFSET || value === PollingMode.FROM_OFFSET;
}

export function isLiveMode(mode?: PollingMode) {
  return mode === PollingMode.TAILING;
}

export const filterOptions = (options: Option[], filter: string) => {
  if (!filter) {
    return options;
  }
  return options.filter(
    ({ value }) => value.toString() && value.toString() === filter
  );
};

export const ADD_FILTER_ID = 'ADD_FILTER';

export function isEditingFilterMode(filterId?: string) {
  return filterId !== ADD_FILTER_ID;
}
