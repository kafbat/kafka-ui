import React from 'react';
import { Button } from 'components/common/Button/Button';
import RefreshIcon from 'components/common/Icons/RefreshIcon';
import {
  RefreshRateSelect,
  RefreshRateStorageKey,
} from 'components/common/RefreshRateSelect/RefreshRateSelect';

type TableRefreshProps = {
  storageKey: RefreshRateStorageKey;
  onRefresh: () => void;
  isFetching?: boolean;
};

const TableRefresh: React.FC<TableRefreshProps> = ({
  storageKey,
  onRefresh,
  isFetching,
}) => {
  return (
    <>
      <Button
        buttonType="secondary"
        buttonSize="M"
        onClick={onRefresh}
        inProgress={isFetching}
      >
        <RefreshIcon /> Refresh
      </Button>
      <RefreshRateSelect storageKey={storageKey} />
    </>
  );
};

export default TableRefresh;
