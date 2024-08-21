import React, { useMemo } from 'react';
import { ClusterName } from 'lib/interfaces/cluster';
import { useNavigate } from 'react-router-dom';
import PageHeading from 'components/common/PageHeading/PageHeading';
import useAppParams from 'lib/hooks/useAppParams';
import Table from 'components/common/NewTable';
import { clusterBrokerPath } from 'lib/paths';
import { useBrokers } from 'lib/hooks/api/brokers';
import { useClusterStats } from 'lib/hooks/api/clusters';

import { BrokersMetrics } from './BrokersMetrics/BrokersMetrics';
import { getBrokersTableColumns, getBrokersTableRows } from './lib';

const BrokersList: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
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
    <>
      <PageHeading clusterName={clusterName} text="Brokers" />

      <BrokersMetrics
        brokerCount={brokerCount}
        inSyncReplicasCount={inSyncReplicasCount}
        outOfSyncReplicasCount={outOfSyncReplicasCount}
        version={version}
        activeControllers={activeControllers}
        offlinePartitionCount={offlinePartitionCount}
        onlinePartitionCount={onlinePartitionCount}
        underReplicatedPartitionCount={underReplicatedPartitionCount}
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
};

export default BrokersList;
