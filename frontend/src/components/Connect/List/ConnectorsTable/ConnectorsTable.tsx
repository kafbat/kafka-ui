import React, { useCallback } from 'react';
import { FullConnectorInfo } from 'generated-sources';
import Table from 'components/common/NewTable';
import { useLocalStoragePersister } from 'components/common/NewTable/ColumnResizer/lib';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';
import { VisibilityState } from '@tanstack/react-table';
import { useFilteredConnectorsDispatch } from 'components/Connect/model/FilteredConnectorsProvider';

import { connectorsColumns } from './connectorsColumns/columns';

const setRowId = (originalRow: FullConnectorInfo) =>
  `${originalRow.name}-${originalRow.connect}`;

type ConnectorsTableProps = {
  connectors: FullConnectorInfo[];
  columnSizingPersistKey?: string;
  columnVisibility?: VisibilityState;
};

export const ConnectorsTable = ({
  connectors,
  columnSizingPersistKey = 'KafkaConnect',
  columnVisibility,
}: ConnectorsTableProps) => {
  const dispath = useFilteredConnectorsDispatch();
  const filterPersister = useQueryPersister(connectorsColumns);
  const columnSizingPersister = useLocalStoragePersister(
    columnSizingPersistKey
  );

  const onFilterRows = useCallback((rows: FullConnectorInfo[]) => {
    dispath({ type: 'updated', connectors: rows });
  }, []);

  return (
    <Table
      data={connectors}
      columns={connectorsColumns}
      enableSorting
      enableColumnResizing
      columnSizingPersister={columnSizingPersister}
      emptyMessage="No connectors found"
      setRowId={setRowId}
      filterPersister={filterPersister}
      columnVisibility={columnVisibility}
      onFilterRows={onFilterRows}
    />
  );
};
