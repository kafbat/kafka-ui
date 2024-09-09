import React, { useMemo, useEffect, useState } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Table from 'components/common/NewTable';
import { Connect, ConnectorState } from 'generated-sources';
import {
  useConnects,
  useConnectors,
  fetchVersion,
} from 'lib/hooks/api/kafkaConnect';
import { ColumnDef, Row } from '@tanstack/react-table';
import toast from 'react-hot-toast';

interface ClusterWithStats extends Connect {
  runningConnectorsCount: number;
  totalConnectorsCount: number;
  runningTasksCount: number;
  totalTasksCount: number;
}

const ConnectorsCell: React.FC<{ row: Row<ClusterWithStats> }> = ({ row }) => {
  const { runningConnectorsCount, totalConnectorsCount } = row.original;

  return (
    <span>
      {totalConnectorsCount === 0
        ? runningConnectorsCount
        : `${runningConnectorsCount} of ${totalConnectorsCount}`}
    </span>
  );
};

const TasksCell: React.FC<{ row: Row<ClusterWithStats> }> = ({ row }) => {
  const { runningTasksCount, totalTasksCount } = row.original;

  return (
    <span>
      {totalTasksCount === 0
        ? runningTasksCount
        : `${runningTasksCount} of ${totalTasksCount}`}
    </span>
  );
};

const ClusterVersionCell: React.FC<{ row: Row<ClusterWithStats> }> = ({
  row,
}) => {
  const { name, address } = row.original;
  const [version, setVersion] = useState<string>('Loading...');

  useEffect(() => {
    const fetchClusterVersion = async () => {
      if (address) {
        const fetchedVersion = await fetchVersion(name, address);
        setVersion(fetchedVersion);
      } else {
        toast.error(
          `Failed to retrieve the version from cluster ${name}, address is missing.`
        );
        setVersion('Unknown');
      }
    };
    fetchClusterVersion().then();
  }, [address]);

  return <span>{version}</span>;
};

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

    if (JSON.stringify(clustersData) !== JSON.stringify(clustersWithStats)) {
      setClustersData(clustersWithStats);
    }
  }, [connects, connectorsMetrics]);

  const columns = useMemo<ColumnDef<ClusterWithStats>[]>(
    () => [
      { header: 'Cluster Name', accessorKey: 'name' },
      { header: 'Version', cell: ClusterVersionCell },
      { header: 'Connectors', cell: ConnectorsCell },
      { header: 'Running Tasks', cell: TasksCell },
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
