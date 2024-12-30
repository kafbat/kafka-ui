import React from 'react';
import {
  Action,
  ConnectorAction,
  ConnectorState,
  FullConnectorInfo,
  ResourceType,
} from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import { ClusterNameRoute } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { Dropdown, DropdownItem } from 'components/common/Dropdown';
import {
  useDeleteConnector,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import { useConfirm } from 'lib/hooks/useConfirm';
import { useIsMutating } from '@tanstack/react-query';
import { ActionDropdownItem } from 'components/common/ActionComponent';

const ActionsCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { connect, name, status } = row.original;
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const mutationsNumber = useIsMutating();
  const isMutating = mutationsNumber > 0;
  const confirm = useConfirm();
  const deleteMutation = useDeleteConnector(clusterName);
  const stateMutation = useUpdateConnectorState(clusterName);
  const handleDelete = () => {
    confirm(
      <>
        Are you sure want to remove <b>{name}</b> connector?
      </>,
      async () => {
        await deleteMutation.mutateAsync({
          props: {
            clusterName,
            connectName: connect,
            connectorName: name,
          },
        });
      }
    );
  };
  // const stateMutation = useUpdateConnectorState(routerProps);
  const performConnectorAction = (action: ConnectorAction) => {
    stateMutation.mutateAsync({
      props: {
        clusterName,
        connectName: connect,
        connectorName: name,
      },
      action,
    });
  };

  return (
    <Dropdown>
      {status.state === ConnectorState.PAUSED ? (
        <ActionDropdownItem
          onClick={() => performConnectorAction(ConnectorAction.RESUME)}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.EDIT,
            value: connect,
          }}
        >
          Resume
        </ActionDropdownItem>
      ) : (
        <ActionDropdownItem
          onClick={() => performConnectorAction(ConnectorAction.PAUSE)}
          disabled={isMutating}
          permission={{
            resource: ResourceType.CONNECT,
            action: Action.EDIT,
            value: name,
          }}
        >
          Pause
        </ActionDropdownItem>
      )}
      <ActionDropdownItem
        onClick={() => performConnectorAction(ConnectorAction.RESTART)}
        disabled={isMutating}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
          value: connect,
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
          value: connect,
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
          value: connect,
        }}
      >
        Restart Failed Tasks
      </ActionDropdownItem>
      <DropdownItem onClick={handleDelete} danger>
        Remove Connector
      </DropdownItem>
    </Dropdown>
  );
};

export default ActionsCell;
