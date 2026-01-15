import React, { useMemo } from 'react';
import { ClusterName } from 'lib/interfaces/cluster';
import { useNavigate } from 'react-router-dom';
import useAppParams from 'lib/hooks/useAppParams';
import Table, {
  exportTableCSV,
  TableProvider,
} from 'components/common/NewTable';
import { clusterBrokerPath } from 'lib/paths';
import { useBrokers } from 'lib/hooks/api/brokers';
import { useClusters, useClusterStats } from 'lib/hooks/api/clusters';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import { Button } from 'components/common/Button/Button';
import ExportIcon from 'components/common/Icons/ExportIcon';

import { getBrokersTableColumns, getBrokersTableRows } from './lib';
import { BrokersMetrics } from './BrokersMetrics/BrokersMetrics';

const BrokersList: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const { data: clusterData } = useClusters();
  const cluster = clusterData?.find(({ name }) => name === clusterName);
  const { data: clusterStats = {} } = useClusterStats(clusterName);
  const { data: brokers } = useBrokers(clusterName);

  const {
    brokerCount,
    activeControllers,
    onlinePartitionCount,
    offlinePartitionCount,
    inSyncReplicasCount,
    outOfSyncReplicasCount,
    underReplicatedPartitionCount,
    diskUsage,
    version,
  } = clusterStats;

  const rows = useMemo(
    () =>
      getBrokersTableRows({
        brokers,
        diskUsage,
        activeControllers,
        onlinePartitionCount,
        offlinePartitionCount,
      }),
    [diskUsage, activeControllers, brokers]
  );

  const columns = useMemo(() => getBrokersTableColumns(), []);

  return (
    <TableProvider>
      {({ table }) => {
        const handleExportClick = () => {
          exportTableCSV(table, { prefix: 'brokers' });
        };

        return (
          <>
            <ResourcePageHeading text="Brokers">
              <Button
                buttonType="secondary"
                buttonSize="M"
                onClick={handleExportClick}
              >
                <ExportIcon /> Export CSV
              </Button>
            </ResourcePageHeading>

            <BrokersMetrics
              brokerCount={brokerCount}
              inSyncReplicasCount={inSyncReplicasCount}
              outOfSyncReplicasCount={outOfSyncReplicasCount}
              version={version}
              activeControllers={activeControllers}
              offlinePartitionCount={offlinePartitionCount}
              onlinePartitionCount={onlinePartitionCount}
              underReplicatedPartitionCount={underReplicatedPartitionCount}
              controller={cluster?.controller}
            />

            <Table
              columns={columns}
              data={rows}
              enableSorting
              onRowClick={({ original: { brokerId } }) =>
                navigate(clusterBrokerPath(clusterName, brokerId))
              }
              emptyMessage="No clusters are online"
            />
          </>
        );
      }}
    </TableProvider>
  );
};

export default BrokersList;
