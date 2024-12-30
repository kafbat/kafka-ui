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

  const deleteConnectorMutation = useDeleteConnector(routerProps.clusterName);
  const deleteConnectorHandler = () =>
    confirm(
      <>
        Are you sure you want to remove <b>{routerProps.connectorName}</b>{' '}
        connector?
      </>,
      async () => {
        try {
          await deleteConnectorMutation.mutateAsync({
            props: routerProps,
          });
          navigate(clusterConnectorsPath(routerProps.clusterName));
        } catch {
          // do not redirect
        }
      }
    );

  const stateMutation = useUpdateConnectorState(routerProps.clusterName);
  const performConnectorAction = (action: ConnectorAction) => {
    stateMutation.mutateAsync({
      props: {
        clusterName: routerProps.clusterName,
        connectName: routerProps.connectName,
        connectorName: routerProps.connectorName,
      },
      action,
    });
  };

  return (
    <S.ConnectorActionsWrapperStyled>
      <Dropdown
        label={
          <S.RestartButton>
            <S.ButtonLabel>Restart</S.ButtonLabel>
            <ChevronDownIcon />
          </S.RestartButton>
        }
      >
        {connector?.status.state === ConnectorState.RUNNING && (
          <ActionDropdownItem
            onClick={() => performConnectorAction(ConnectorAction.PAUSE)}
            disabled={isMutating}
            permission={{
              resource: ResourceType.CONNECT,
              action: Action.EDIT,
              value: routerProps.connectName,
            }}
          >
            Pause
          </ActionDropdownItem>
        )}
        {connector?.status.state === ConnectorState.PAUSED && (
          <ActionDropdownItem
            onClick={() => performConnectorAction(ConnectorAction.RESUME)}
            disabled={isMutating}
            permission={{
              resource: ResourceType.CONNECT,
              action: Action.EDIT,
              value: routerProps.connectName,
            }}
          >
            Resume
          </ActionDropdownItem>
        )}
        <ActionDropdownItem
          onClick={() => performConnectorAction(ConnectorAction.RESTART)}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.RESTART,
            value: routerProps.connectName,
          }}
        >
          Restart Connector
        </ActionDropdownItem>
        <ActionDropdownItem
          onClick={() =>
            performConnectorAction(ConnectorAction.RESTART_ALL_TASKS)
          }
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.RESTART,
            value: routerProps.connectName,
          }}
        >
          Restart All Tasks
        </ActionDropdownItem>
        <ActionDropdownItem
          onClick={() =>
            performConnectorAction(ConnectorAction.RESTART_FAILED_TASKS)
          }
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.RESTART,
            value: routerProps.connectName,
          }}
        >
          Restart Failed Tasks
        </ActionDropdownItem>
      </Dropdown>
      <Dropdown>
        <ActionDropdownItem
          onClick={deleteConnectorHandler}
          disabled={isMutating}
          danger
          permission={{
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
