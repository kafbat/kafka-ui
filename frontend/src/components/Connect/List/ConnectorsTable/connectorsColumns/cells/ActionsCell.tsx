import React, { useContext } from 'react';
import {
  Action,
  ConnectorAction,
  ConnectorState,
  FullConnectorInfo,
  ResourceType,
} from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import { RouteParamsClusterTopic } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { Dropdown } from 'components/common/Dropdown';
import {
  useDeleteConnector,
  useResetConnectorOffsets,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import { useConfirm } from 'lib/hooks/useConfirm';
import { useIsMutating } from '@tanstack/react-query';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import ClusterContext from 'components/contexts/ClusterContext';

const ActionsCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { connect, name, status } = row.original;
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { isReadOnly } = useContext(ClusterContext);
  const mutationsNumber = useIsMutating();
  const isMutating = mutationsNumber > 0;
  const confirm = useConfirm();
  const deleteMutation = useDeleteConnector({
    clusterName,
    connectName: connect,
    connectorName: name,
    topicName,
  });
  const stateMutation = useUpdateConnectorState({
    clusterName,
    connectName: connect,
    connectorName: name,
    topicName,
  });
  const resetConnectorOffsetsMutation = useResetConnectorOffsets({
    clusterName,
    connectName: connect,
    connectorName: name,
    topicName,
  });
  const handleDelete = () => {
    confirm(
      <>
        Are you sure you want to remove the <b>{name}</b> connector?
      </>,
      async () => {
        await deleteMutation.mutateAsync();
      }
    );
  };

  const resumeConnectorHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.RESUME);
  const restartConnectorHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.RESTART);

  const restartAllTasksHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.RESTART_ALL_TASKS);

  const restartFailedTasksHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.RESTART_FAILED_TASKS);

  const pauseConnectorHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.PAUSE);

  const stopConnectorHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.STOP);

  const resetOffsetsHandler = () => {
    confirm(
      <>
        Are you sure you want to reset the <b>{name}</b> connector offsets?
      </>,
      () => resetConnectorOffsetsMutation.mutateAsync()
    );
  };

  return (
    <Dropdown>
      {(status.state === ConnectorState.PAUSED ||
        status.state === ConnectorState.STOPPED) && (
        <ActionDropdownItem
          onClick={resumeConnectorHandler}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: connect,
          }}
        >
          Resume
        </ActionDropdownItem>
      )}
      {status.state === ConnectorState.RUNNING && (
        <ActionDropdownItem
          onClick={pauseConnectorHandler}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: connect,
          }}
        >
          Pause
        </ActionDropdownItem>
      )}
      {status.state === ConnectorState.RUNNING && (
        <ActionDropdownItem
          onClick={stopConnectorHandler}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: connect,
          }}
        >
          Stop
        </ActionDropdownItem>
      )}
      <ActionDropdownItem
        onClick={restartConnectorHandler}
        disabled={isMutating || isReadOnly}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.OPERATE,
          value: connect,
        }}
      >
        Restart Connector
      </ActionDropdownItem>
      <ActionDropdownItem
        onClick={restartAllTasksHandler}
        disabled={isMutating}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.OPERATE,
          value: connect,
        }}
      >
        Restart All Tasks
      </ActionDropdownItem>
      <ActionDropdownItem
        onClick={restartFailedTasksHandler}
        disabled={isMutating}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.OPERATE,
          value: connect,
        }}
      >
        Restart Failed Tasks
      </ActionDropdownItem>
      <ActionDropdownItem
        onClick={resetOffsetsHandler}
        disabled={isMutating || status.state !== ConnectorState.STOPPED}
        danger
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESET_OFFSETS,
          value: connect,
        }}
      >
        Reset Offsets
      </ActionDropdownItem>
      <ActionDropdownItem
        onClick={handleDelete}
        danger
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.DELETE,
          value: connect,
        }}
      >
        Delete
      </ActionDropdownItem>
    </Dropdown>
  );
};

export default ActionsCell;
