import React, { useMemo } from 'react';
import PageHeading from 'components/common/PageHeading/PageHeading';
import * as Metrics from 'components/common/Metrics';
import { Tag } from 'components/common/Tag/Tag.styled';
import Switch from 'components/common/Switch/Switch';
import { useClusters } from 'lib/hooks/api/clusters';
import { Cluster, ResourceType, ServerStatus } from 'generated-sources';
import { ColumnDef, Row } from '@tanstack/react-table';
import Table, { SizeCell } from 'components/common/NewTable';
import useBoolean from 'lib/hooks/useBoolean';
import { clusterBrokersPath, clusterNewConfigPath } from 'lib/paths';
import { GlobalSettingsContext } from 'components/contexts/GlobalSettingsContext';
import { ActionCanButton } from 'components/common/ActionComponent';
import { useGetUserInfo } from 'lib/hooks/api/roles';
import { useLocalStoragePersister } from 'components/common/NewTable/ColumnResizer/lib';
import { useNavigate } from 'react-router-dom';

import * as S from './Dashboard.styled';
import ClusterName from './ClusterName';
import ClusterTableActionsCell from './ClusterTableActionsCell';

const Dashboard: React.FC = () => {
  const { data } = useGetUserInfo();
  const navigate = useNavigate();
  const clusters = useClusters();
  const { value: showOfflineOnly, toggle } = useBoolean(false);
  const appInfo = React.useContext(GlobalSettingsContext);

  const config = React.useMemo(() => {
    const clusterList = clusters.data || [];
    const offlineClusters = clusterList.filter(
      ({ status }) => status === ServerStatus.OFFLINE
    );
    return {
      list: showOfflineOnly ? offlineClusters : clusterList,
      online: clusterList.length - offlineClusters.length,
      offline: offlineClusters.length,
    };
  }, [clusters, showOfflineOnly]);

  const columns = React.useMemo<ColumnDef<Cluster>[]>(() => {
    const initialColumns: ColumnDef<Cluster>[] = [
      {
        header: 'Cluster name',
        accessorKey: 'name',
        cell: ClusterName,
        meta: { width: '100%' },
        enableResizing: true,
      },
      { header: 'Version', accessorKey: 'version', size: 100 },
      {
        header: 'Brokers count',
        accessorKey: 'brokerCount',
        size: 120,
      },
      {
        header: 'Partitions',
        accessorKey: 'onlinePartitionCount',
        size: 100,
      },
      { header: 'Topics', accessorKey: 'topicCount', size: 80 },
      {
        header: 'Production',
        accessorKey: 'bytesInPerSec',
        cell: SizeCell,
        size: 100,
      },
      {
        header: 'Consumption',
        accessorKey: 'bytesOutPerSec',
        cell: SizeCell,
        size: 116,
      },
    ];

    if (appInfo.hasDynamicConfig) {
      initialColumns.push({
        header: '',
        id: 'actions',
        cell: ClusterTableActionsCell,
        size: 140,
      });
    }

    return initialColumns;
  }, []);

  const hasPermissions = useMemo(() => {
    if (!data?.rbacEnabled) return true;
    return !!data?.userInfo?.permissions.some(
      (permission) => permission.resource === ResourceType.APPLICATIONCONFIG
    );
  }, [data]);

  const columnSizingPersister = useLocalStoragePersister('KafkaConnect');

  const onRowClick = (row: Row<Cluster>) => {
    navigate(clusterBrokersPath(row.original.name));
  };

  return (
    <>
      <PageHeading text="Dashboard" />
      <Metrics.Wrapper>
        <Metrics.Section>
          <Metrics.Indicator label={<Tag color="green">Online</Tag>}>
            <span>{config.online || 0}</span>{' '}
            <Metrics.LightText>clusters</Metrics.LightText>
          </Metrics.Indicator>
          <Metrics.Indicator label={<Tag color="gray">Offline</Tag>}>
            <span>{config.offline || 0}</span>{' '}
            <Metrics.LightText>clusters</Metrics.LightText>
          </Metrics.Indicator>
        </Metrics.Section>
      </Metrics.Wrapper>
      <S.Toolbar>
        <div>
          <Switch
            name="switchRoundedDefault"
            checked={showOfflineOnly}
            onChange={toggle}
          />
          <label>Only offline clusters</label>
        </div>
        {appInfo.hasDynamicConfig && (
          <ActionCanButton
            buttonType="primary"
            buttonSize="M"
            to={clusterNewConfigPath}
            canDoAction={hasPermissions}
          >
            Configure new cluster
          </ActionCanButton>
        )}
      </S.Toolbar>
      <Table
        onRowClick={onRowClick}
        columns={columns}
        data={config?.list}
        enableSorting
        enableColumnResizing
        columnSizingPersister={columnSizingPersister}
        emptyMessage={clusters.isFetched ? 'No clusters found' : 'Loading...'}
      />
    </>
  );
};

export default Dashboard;
