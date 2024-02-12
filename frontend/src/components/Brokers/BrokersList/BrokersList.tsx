import React, { useMemo } from 'react';
import { ClusterName } from 'redux/interfaces';
import { useNavigate } from 'react-router-dom';
import PageHeading from 'components/common/PageHeading/PageHeading';
import useAppParams from 'lib/hooks/useAppParams';
import Table from 'components/common/NewTable';
import { clusterBrokerPath } from 'lib/paths';
import { useClusterStats } from 'lib/hooks/api/clusters';
import { useBrokers } from 'lib/hooks/api/brokers';

import { BrokersMetrics } from './BrokersMetrics/BrokersMetrics';
import { getBrokersTableColumns, getBrokersTableRows } from './lib';

// const brokers = JSON.parse(
//   '[\n' +
//     '    {\n' +
//     '        "id": 1,\n' +
//     '        "host": "test-region-1-broker-1.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 18,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": -1.8\n' +
//     '    },\n' +
//     '    {\n' +
//     '        "id": 2,\n' +
//     '        "host": "test-region-1-broker-2.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 18,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": -1.8\n' +
//     '    },\n' +
//     '    {\n' +
//     '        "id": 3,\n' +
//     '        "host": "test-region-1-broker-3.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 18,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": -1.8\n' +
//     '    },\n' +
//     '    {\n' +
//     '        "id": 4,\n' +
//     '        "host": "test-region-2-broker-1.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 19,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": 3.6\n' +
//     '    },\n' +
//     '    {\n' +
//     '        "id": 5,\n' +
//     '        "host": "test-region-2-broker-2.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 18,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": -1.8\n' +
//     '    },\n' +
//     '    {\n' +
//     '        "id": 6,\n' +
//     '        "host": "test-region-2-broker-3.kafka",\n' +
//     '        "port": 9092,\n' +
//     '        "bytesInPerSec": null,\n' +
//     '        "bytesOutPerSec": null,\n' +
//     '        "partitionsLeader": 19,\n' +
//     '        "partitions": 110,\n' +
//     '        "inSyncPartitions": 110,\n' +
//     '        "partitionsSkew": 0.0,\n' +
//     '        "leadersSkew": 3.6\n' +
//     '    }  \n' +
//     ']'
// ) as Broker[] | undefined;
//
// const clusterStats = JSON.parse(
//   '{\n' +
//     '    "brokerCount": 6,\n' +
//     '    "zooKeeperStatus": null,\n' +
//     '    "activeControllers": 3,\n' +
//     '    "onlinePartitionCount": 110,\n' +
//     '    "offlinePartitionCount": 0, \n' +
//     '    "inSyncReplicasCount": 660,\n' +
//     '    "outOfSyncReplicasCount": 0,\n' +
//     '    "underReplicatedPartitionCount": 0,\n' +
//     '    "diskUsage": [\n' +
//     '        {\n' +
//     '            "brokerId": 1, \n' +
//     '            "segmentSize": 15296,\n' +
//     '            "segmentCount": 110\n' +
//     '        },\n' +
//     '        {\n' +
//     '            "brokerId": 2, \n' +
//     '            "segmentSize": 15296,\n' +
//     '            "segmentCount": 110\n' +
//     '        },\n' +
//     '        {\n' +
//     '            "brokerId": 3, \n' +
//     '            "segmentSize": 15296,\n' +
//     '            "segmentCount": 110\n' +
//     '        },\n' +
//     '        {\n' +
//     '            "brokerId": 4, \n' +
//     '            "segmentSize": 15608,\n' +
//     '            "segmentCount": 130\n' +
//     '        },\n' +
//     '        {\n' +
//     '            "brokerId": 5, \n' +
//     '            "segmentSize": 15892,\n' +
//     '            "segmentCount": 131\n' +
//     '        },\n' +
//     '        {\n' +
//     '            "brokerId": 6, \n' +
//     '            "segmentSize": 15608,\n' +
//     '            "segmentCount": 132\n' +
//     '        }\n' +
//     '    ],\n' +
//     '    "version": "2.7-IV2"\n' +
//     '}'
// ) as ClusterStats;

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
    () => getBrokersTableRows({ brokers, diskUsage, activeControllers }),
    [diskUsage, activeControllers, brokers]
  );

  const columns = useMemo(() => getBrokersTableColumns(), []);

  return (
    <>
      <PageHeading text="Brokers" />

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
