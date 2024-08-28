import React, { useMemo, useEffect, useState } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Table from 'components/common/NewTable';
import { Connect, ConnectorState } from 'generated-sources';
import { useConnects, useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';

interface ClusterWithStats extends Connect {
  runningConnectorsCount: number;
  totalConnectorsCount: number;
  runningTasksCount: number;
  totalTasksCount: number;
}

const ClustersList: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connects = [] } = useConnects(clusterName);
  const { data: connectorsMetrics = [] } = useConnectors(clusterName);

  const [clustersData, setClustersData] = useState<ClusterWithStats[]>([]);

  useEffect(() => {
    const clustersWithStats = connects.map((connect) => {
      const relatedConnectors = connectorsMetrics.filter(
        (connector) => connector.connect === connect.name
      );

      const runningConnectorsCount = relatedConnectors.filter(
        (connector) => connector.status.state === ConnectorState.RUNNING
      ).length;

      const totalConnectorsCount = relatedConnectors.length;

      const totalTasksCount = relatedConnectors.reduce(
        (sum, connector) => sum + (connector.tasksCount || 0),
        0
      );

      const runningTasksCount =
        totalTasksCount -
        relatedConnectors.reduce(
          (sum, connector) => sum + (connector.failedTasksCount || 0),
          0
        );

      return {
        ...connect,
        runningConnectorsCount,
        totalConnectorsCount,
        runningTasksCount,
        totalTasksCount,
      };
    });

    setClustersData(clustersWithStats);
  }, [connects, connectorsMetrics]);

  const columns = useMemo<ColumnDef<ClusterWithStats>[]>(
    () => [
      { header: 'Cluster Name', accessorKey: 'name' },
      { header: 'Version', accessorKey: 'address' },
      {
        header: 'Connectors',
        cell: ({ row }) => (
          <>
            {row.original.runningConnectorsCount} / {row.original.totalConnectorsCount}
          </>
        ),
      },
      {
        header: 'Tasks',
        cell: ({ row }) => (
          <>
            {row.original.runningTasksCount} / {row.original.totalTasksCount}
          </>
        ),
      },
    ],
    []
  );

  return (
    <Table
      data={clustersData}
      columns={columns}
      enableSorting
      emptyMessage="No clusters found"
      setRowId={(originalRow) => originalRow.name}
    />
  );
};

export default ClustersList;
