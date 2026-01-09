import React from 'react';
import * as Metrics from 'components/common/Metrics';
import { ControllerType } from 'generated-sources';

import * as S from './BrokersMetrics.styled';

type BrokersMetricsProps = {
  brokerCount: number | undefined;
  inSyncReplicasCount: number | undefined;
  outOfSyncReplicasCount: number | undefined;
  offlinePartitionCount: number | undefined;
  activeControllers: number | undefined;
  onlinePartitionCount: number | undefined;
  underReplicatedPartitionCount: number | undefined;
  version: string | undefined;
  controller: ControllerType | undefined;
};

export const BrokersMetrics = ({
  brokerCount,
  version,
  activeControllers,
  outOfSyncReplicasCount,
  inSyncReplicasCount,
  offlinePartitionCount,
  underReplicatedPartitionCount,
  onlinePartitionCount,
  controller,
}: BrokersMetricsProps) => {
  const replicas = (inSyncReplicasCount ?? 0) + (outOfSyncReplicasCount ?? 0);
  const areAllInSync = inSyncReplicasCount && replicas === inSyncReplicasCount;
  const partitionIsOffline = offlinePartitionCount && offlinePartitionCount > 0;

  const isActiveControllerUnKnown = typeof activeControllers === 'undefined';

  return (
    <Metrics.Wrapper>
      <Metrics.Section title="Uptime">
        <Metrics.Indicator label="Broker Count">
          {brokerCount}
        </Metrics.Indicator>

        <Metrics.Indicator
          label="Active Controller"
          isAlert={isActiveControllerUnKnown}
        >
          {isActiveControllerUnKnown ? (
            <S.DangerText>No Active Controller</S.DangerText>
          ) : (
            activeControllers
          )}
        </Metrics.Indicator>

        <Metrics.Indicator label="Version">{version}</Metrics.Indicator>
      </Metrics.Section>

      <Metrics.Section title="Partitions">
        <Metrics.Indicator
          label="Online"
          isAlert
          alertType={partitionIsOffline ? 'error' : 'success'}
        >
          {partitionIsOffline ? (
            <Metrics.RedText>{onlinePartitionCount}</Metrics.RedText>
          ) : (
            onlinePartitionCount
          )}
          <Metrics.LightText>
            {` of ${(onlinePartitionCount || 0) + (offlinePartitionCount || 0)}
              `}
          </Metrics.LightText>
        </Metrics.Indicator>

        <Metrics.Indicator
          label="URP"
          title="Under replicated partitions"
          isAlert
          alertType={!underReplicatedPartitionCount ? 'success' : 'error'}
        >
          {!underReplicatedPartitionCount ? (
            <Metrics.LightText>
              {underReplicatedPartitionCount}
            </Metrics.LightText>
          ) : (
            <Metrics.RedText>{underReplicatedPartitionCount}</Metrics.RedText>
          )}
        </Metrics.Indicator>

        <Metrics.Indicator
          label="In Sync Replicas"
          isAlert
          alertType={areAllInSync ? 'success' : 'error'}
        >
          {areAllInSync ? (
            replicas
          ) : (
            <Metrics.RedText>{inSyncReplicasCount}</Metrics.RedText>
          )}
          <Metrics.LightText> of {replicas}</Metrics.LightText>
        </Metrics.Indicator>

        <Metrics.Indicator label="Out Of Sync Replicas">
          {outOfSyncReplicasCount}
        </Metrics.Indicator>

        <Metrics.Indicator label="Controller Type">
          {controller === ControllerType.KRAFT ? 'KRaft' : 'ZooKeeper'}
        </Metrics.Indicator>
      </Metrics.Section>
    </Metrics.Wrapper>
  );
};
