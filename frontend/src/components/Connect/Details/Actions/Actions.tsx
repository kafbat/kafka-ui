import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useIsMutating } from '@tanstack/react-query';
import {
  Action,
  ConnectorAction,
  ConnectorState,
  ResourceType,
} from 'generated-sources';
import useAppParams from 'lib/hooks/useAppParams';
import {
  useConnector,
  useDeleteConnector,
  useResetConnectorOffsets,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import {
  clusterConnectorsPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import { useConfirm } from 'lib/hooks/useConfirm';
import { Dropdown } from 'components/common/Dropdown';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import ChevronDownIcon from 'components/common/Icons/ChevronDownIcon';

import * as S from './Action.styled';

const Actions: React.FC = () => {
  const navigate = useNavigate();
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const mutationsNumber = useIsMutating();
  const isMutating = mutationsNumber > 0;

  const { data: connector } = useConnector(routerProps);
  const confirm = useConfirm();

  const deleteConnectorMutation = useDeleteConnector(routerProps);
  const deleteConnectorHandler = () =>
    confirm(
      <>
        Are you sure you want to remove the <b>{routerProps.connectorName}</b>{' '}
        connector?
      </>,
      async () => {
        try {
          await deleteConnectorMutation.mutateAsync();
          navigate(clusterConnectorsPath(routerProps.clusterName));
        } catch {
          // do not redirect
        }
      }
    );

  const stateMutation = useUpdateConnectorState(routerProps);
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
  const resumeConnectorHandler = () =>
    stateMutation.mutateAsync(ConnectorAction.RESUME);

  const resetConnectorOffsetsMutation = useResetConnectorOffsets(routerProps);
  const resetConnectorOffsetsHandler = () =>
    confirm(
      <>
        Are you sure you want to reset the <b>{routerProps.connectorName}</b>{' '}
        connector offsets?
      </>,
      () => resetConnectorOffsetsMutation.mutateAsync()
    );

  const connectorPath = `${routerProps.connectName}/${routerProps.connectorName}`;

  return (
    <S.ConnectorActionsWrapperStyled>
      <Dropdown
        disabled={isMutating}
        label={
          <S.RestartButton>
            <S.ButtonLabel>Restart</S.ButtonLabel>
            <ChevronDownIcon />
          </S.RestartButton>
        }
      >
        {connector?.status.state === ConnectorState.RUNNING && (
          <ActionDropdownItem
            onClick={pauseConnectorHandler}
            permission={{
              resource: ResourceType.CONNECTOR,
              action: Action.OPERATE,
              value: connectorPath,
            }}
            fallbackPermission={{
              resource: ResourceType.CONNECT,
              action: Action.OPERATE,
              value: routerProps.connectName,
            }}
          >
            Pause
          </ActionDropdownItem>
        )}
        {connector?.status.state === ConnectorState.RUNNING && (
          <ActionDropdownItem
            onClick={stopConnectorHandler}
            permission={{
              resource: ResourceType.CONNECTOR,
              action: Action.OPERATE,
              value: connectorPath,
            }}
            fallbackPermission={{
              resource: ResourceType.CONNECT,
              action: Action.OPERATE,
              value: routerProps.connectName,
            }}
          >
            Stop
          </ActionDropdownItem>
        )}
        {(connector?.status.state === ConnectorState.PAUSED ||
          connector?.status.state === ConnectorState.STOPPED) && (
          <ActionDropdownItem
            onClick={resumeConnectorHandler}
            permission={{
              resource: ResourceType.CONNECTOR,
              action: Action.OPERATE,
              value: connectorPath,
            }}
            fallbackPermission={{
              resource: ResourceType.CONNECT,
              action: Action.OPERATE,
              value: routerProps.connectName,
            }}
          >
            Resume
          </ActionDropdownItem>
        )}
        <ActionDropdownItem
          onClick={restartConnectorHandler}
          permission={{
            resource: ResourceType.CONNECTOR,
            action: Action.OPERATE,
            value: connectorPath,
          }}
          fallbackPermission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: routerProps.connectName,
          }}
        >
          Restart Connector
        </ActionDropdownItem>
        <ActionDropdownItem
          onClick={restartAllTasksHandler}
          permission={{
            resource: ResourceType.CONNECTOR,
            action: Action.OPERATE,
            value: connectorPath,
          }}
          fallbackPermission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: routerProps.connectName,
          }}
        >
          Restart All Tasks
        </ActionDropdownItem>
        <ActionDropdownItem
          onClick={restartFailedTasksHandler}
          permission={{
            resource: ResourceType.CONNECTOR,
            action: Action.OPERATE,
            value: connectorPath,
          }}
          fallbackPermission={{
            resource: ResourceType.CONNECT,
            action: Action.OPERATE,
            value: routerProps.connectName,
          }}
        >
          Restart Failed Tasks
        </ActionDropdownItem>
      </Dropdown>
      <Dropdown>
        <ActionDropdownItem
          onClick={resetConnectorOffsetsHandler}
          disabled={
            isMutating || connector?.status.state !== ConnectorState.STOPPED
          }
          danger
          permission={{
            resource: ResourceType.CONNECTOR,
            action: Action.RESET_OFFSETS,
            value: connectorPath,
          }}
          fallbackPermission={{
            resource: ResourceType.CONNECT,
            action: Action.RESET_OFFSETS,
            value: routerProps.connectName,
          }}
        >
          Reset Offsets
        </ActionDropdownItem>
        <ActionDropdownItem
          onClick={deleteConnectorHandler}
          danger
          permission={{
            resource: ResourceType.CONNECTOR,
            action: Action.DELETE,
            value: connectorPath,
          }}
          fallbackPermission={{
            resource: ResourceType.CONNECT,
            action: Action.DELETE,
            value: routerProps.connectName,
          }}
        >
          Delete
        </ActionDropdownItem>
      </Dropdown>
    </S.ConnectorActionsWrapperStyled>
  );
};

export default Actions;
