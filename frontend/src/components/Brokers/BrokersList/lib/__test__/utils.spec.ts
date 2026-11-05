import {
  getBrokersTableColumns,
  getBrokersTableRows,
} from 'components/Brokers/BrokersList/lib/utils';
import { BrokersTableRow } from 'components/Brokers/BrokersList/lib/types';
import { brokersPayload } from 'lib/fixtures/brokers';
import { clusterStatsPayload } from 'lib/fixtures/clusters';

type CsvFn = (row: BrokersTableRow) => string;

const diskUsageCsv = (): CsvFn => {
  const column = getBrokersTableColumns().find(
    (col) => col.header === 'Disk usage'
  );
  return (column?.meta as { csvFn: CsvFn }).csvFn;
};

const row = (overrides: Partial<BrokersTableRow>): BrokersTableRow => ({
  brokerId: 1,
  size: 1234,
  count: 12,
  port: 9092,
  host: 'localhost',
  partitionsLeader: 1,
  leadersSkew: 0,
  replicas: 1,
  inSyncReplicas: 1,
  replicasSkew: 0,
  activeControllers: 1,
  ...overrides,
});

describe('BrokersList utils', () => {
  describe('getBrokersTableRows', () => {
    it('copies the segment count from disk usage as is', () => {
      const rows = getBrokersTableRows({
        brokers: brokersPayload,
        diskUsage: clusterStatsPayload.diskUsage,
        activeControllers: 1,
        onlinePartitionCount: 1,
        offlinePartitionCount: 0,
      });
      expect(rows[0].count).toEqual(
        clusterStatsPayload.diskUsage[0].segmentCount
      );
      expect(rows[0].size).toEqual(
        clusterStatsPayload.diskUsage[0].segmentSize
      );
    });

    it('leaves the count undefined when disk usage has no segment count', () => {
      const rows = getBrokersTableRows({
        brokers: brokersPayload,
        diskUsage: clusterStatsPayload.diskUsage.map(
          ({ brokerId, segmentSize }) => ({ brokerId, segmentSize })
        ),
        activeControllers: 1,
        onlinePartitionCount: 1,
        offlinePartitionCount: 0,
      });
      expect(rows[0].count).toBeUndefined();
      expect(rows[0].size).toEqual(
        clusterStatsPayload.diskUsage[0].segmentSize
      );
    });
  });

  describe('disk usage CSV export', () => {
    it('includes the segment count when known', () => {
      expect(diskUsageCsv()(row({ size: 1234, count: 12 }))).toEqual(
        '1.21 KB, 12 segment(s)'
      );
    });

    it('treats zero as a real count', () => {
      expect(diskUsageCsv()(row({ size: 2048, count: 0 }))).toEqual(
        '2 KB, 0 segment(s)'
      );
    });

    it('exports only the size when the count is undefined', () => {
      expect(diskUsageCsv()(row({ size: 1234, count: undefined }))).toEqual(
        '1.21 KB'
      );
    });

    it('exports only the size when the count is null', () => {
      // the raw API can send null for segmentCount when metrics are not configured
      expect(
        diskUsageCsv()(row({ size: 1234, count: null as unknown as undefined }))
      ).toEqual('1.21 KB');
    });

    it('exports N/A when the size is unknown', () => {
      expect(
        diskUsageCsv()(row({ size: undefined, count: undefined }))
      ).toEqual('N/A');
    });
  });
});
