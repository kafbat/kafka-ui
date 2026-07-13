import React from 'react';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import Dropdown from 'components/common/Dropdown/Dropdown';
import DropdownItem from 'components/common/Dropdown/DropdownItem';
import RefreshIcon from 'components/common/Icons/RefreshIcon';
import DropdownArrowIcon from 'components/common/Icons/DropdownArrowIcon';
import Spinner from 'components/common/Spinner/Spinner';
import { RefreshRateStorageKey } from 'components/common/RefreshRateSelect/RefreshRateSelect';

import * as S from './TableRefresh.styled';

type TableRefreshProps = {
  storageKey: RefreshRateStorageKey;
  onRefresh: () => void;
  isFetching?: boolean;
};

const OPTIONS = [
  { value: 0, label: 'Off' },
  { value: 2, label: '2s' },
  { value: 5, label: '5s' },
  { value: 10, label: '10s' },
  { value: 15, label: '15s' },
];

/**
 * Grafana-style split control: the left segment triggers a manual refresh
 * (spinner while fetching); the right segment shows the active auto-refresh
 * interval and opens a menu to change it. The interval is persisted under
 * `storageKey` and read back by `useRefreshRate` to drive the query polling.
 */
const TableRefresh: React.FC<TableRefreshProps> = ({
  storageKey,
  onRefresh,
  isFetching,
}) => {
  const [rate, setRate] = useLocalStorage<number>(storageKey, 0);
  const current = OPTIONS.find((option) => option.value === rate);

  return (
    <S.Group>
      <S.RefreshButton
        type="button"
        onClick={onRefresh}
        disabled={isFetching}
        aria-label="Refresh"
      >
        {isFetching ? <Spinner size={16} borderWidth={2} /> : <RefreshIcon />}
        Refresh
      </S.RefreshButton>
      <Dropdown
        aria-label="Auto refresh interval"
        openBtnEl={
          <S.IntervalButton type="button">
            {rate > 0 && <S.Rate>{current?.label}</S.Rate>}
            <DropdownArrowIcon isOpen={false} />
          </S.IntervalButton>
        }
      >
        {OPTIONS.map((option) => (
          <DropdownItem
            key={option.value}
            onClick={() => setRate(option.value)}
          >
            {option.value === 0 ? 'Off' : `Every ${option.label}`}
          </DropdownItem>
        ))}
      </Dropdown>
    </S.Group>
  );
};

export default TableRefresh;
