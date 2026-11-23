import { Option } from 'react-multi-select-component';
import {
  ADD_FILTER_ID,
  filterOptions,
  isEditingFilterMode,
  isLiveMode,
  isModeOffsetSelector,
  isModeOptionWithInput,
} from 'components/Topics/Topic/Messages/Filters/utils';
import { PollingMode } from 'generated-sources';

const options: Option[] = [
  {
    value: 0,
    label: 'Partition #0',
  },
  {
    value: 1,
    label: 'Partition #1',
  },
  {
    value: 11,
    label: 'Partition #11',
  },
  {
    value: 21,
    label: 'Partition #21',
  },
];

describe('utils', () => {
  describe('filterOptions', () => {
    it('returns options if no filter is defined', () => {
      expect(filterOptions(options, '')).toEqual(options);
    });

    it('returns filtered options', () => {
      expect(filterOptions(options, '11')).toEqual([options[2]]);
    });
  });

  describe('isModeOptionWithInput', () => {
    describe('check the validity if Mode offset Selector only during', () => {
      expect(isModeOptionWithInput(PollingMode.TAILING)).toBeFalsy();
      expect(isModeOptionWithInput(PollingMode.LATEST)).toBeFalsy();
      expect(isModeOptionWithInput(PollingMode.EARLIEST)).toBeFalsy();
      expect(isModeOptionWithInput(PollingMode.FROM_TIMESTAMP)).toBeTruthy();
      expect(isModeOptionWithInput(PollingMode.TO_TIMESTAMP)).toBeTruthy();
      expect(isModeOptionWithInput(PollingMode.FROM_OFFSET)).toBeTruthy();
      expect(isModeOptionWithInput(PollingMode.TO_OFFSET)).toBeTruthy();
    });
  });

  describe('isModeOffsetSelector', () => {
    it('check the validity if Mode offset Selector only during', () => {
      expect(isModeOffsetSelector(PollingMode.TAILING)).toBeFalsy();
      expect(isModeOffsetSelector(PollingMode.LATEST)).toBeFalsy();
      expect(isModeOffsetSelector(PollingMode.EARLIEST)).toBeFalsy();
      expect(isModeOffsetSelector(PollingMode.FROM_TIMESTAMP)).toBeFalsy();
      expect(isModeOffsetSelector(PollingMode.TO_TIMESTAMP)).toBeFalsy();
      expect(isModeOffsetSelector(PollingMode.FROM_OFFSET)).toBeTruthy();
      expect(isModeOffsetSelector(PollingMode.TO_OFFSET)).toBeTruthy();
    });
  });

  describe('isLiveMode', () => {
    it('should check the validity of data on;y during tailing mode', () => {
      expect(isLiveMode(PollingMode.TAILING)).toBeTruthy();
      expect(isLiveMode(PollingMode.LATEST)).toBeFalsy();
      expect(isLiveMode(PollingMode.EARLIEST)).toBeFalsy();
      expect(isLiveMode(PollingMode.FROM_TIMESTAMP)).toBeFalsy();
      expect(isLiveMode(PollingMode.TO_TIMESTAMP)).toBeFalsy();
      expect(isLiveMode(PollingMode.FROM_OFFSET)).toBeFalsy();
      expect(isLiveMode(PollingMode.TO_OFFSET)).toBeFalsy();
    });
  });

  describe('isEditingFilterMode', () => {
    it('should editing value', () => {
      expect(isEditingFilterMode('testing')).toBeTruthy();
      expect(isEditingFilterMode(ADD_FILTER_ID)).toBeFalsy();
    });
  });
});
