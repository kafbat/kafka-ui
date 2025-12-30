import React from 'react';
import Select from 'components/common/Select/Select';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';

const options = [
  { value: 0, label: 'Off' },
  { value: 2, label: '2 sec' },
  { value: 5, label: '5 sec' },
  { value: 10, label: '10 sec' },
  { value: 15, label: '15 sec' },
];

type RefreshRateSelectProps = {
  storageKey: 'consumer-groups-refresh-rate' | 'topics-refresh-rate';
};

export const RefreshRateSelect = ({ storageKey }: RefreshRateSelectProps) => {
  const [rate, setRate] = useLocalStorage<number>(storageKey, 0);

  return (
    <Select
      formatSelectedOption={(option) => `Refresh rate: ${option.label}`}
      minWidth="180px"
      selectSize="M"
      onChange={setRate}
      value={rate}
      options={options}
      defaultValue={0}
    />
  );
};
