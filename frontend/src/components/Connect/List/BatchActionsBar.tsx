import React from 'react';
import {
  Action,
  ResourceType,
  ConnectorAction,
  FullConnectorInfo,
} from 'generated-sources';
import { Row } from '@tanstack/react-table';
import { ActionButton } from 'components/common/ActionComponent';
import { useConfirm } from 'lib/hooks/useConfirm';
import {
  useDeleteConnector,
  useUpdateConnectorState,
} from 'lib/hooks/api/kafkaConnect';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import { showServerError } from 'lib/errorHandling';

interface BatchActionsBarProps {
  rows: Row<FullConnectorInfo>[];
  resetRowSelection(): void;
}

const BatchActionsBar: React.FC<BatchActionsBarProps> = ({
  rows,
  resetRowSelection,
}) => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const selectedConnectors = rows.map(({ original }) => original);
  const stateMutation = useUpdateConnectorState(clusterName);
  const deleteMutation = useDeleteConnector(clusterName);
  const confirm = useConfirm();

  const performBatchConnectorsAction = async (
    confirmMessage: string,
    action: ConnectorAction
  ) => {
    confirm(confirmMessage, async () => {
      try {
        await Promise.all(
          selectedConnectors.map(({ connect, name }) =>
            stateMutation.mutateAsync({
              props: {
                clusterName,
                connectName: connect,
                connectorName: name,
              },
              action,
            })
          )
        );
      } catch (error) {
        showServerError(error as Response);
      } finally {
        resetRowSelection();
      }
    });
  };

  const deleteConnectorsFailedTasksHandler = async () => {
    confirm(
      'Are you sure you want to delete selected connectors?',
      async () => {
        try {
          await Promise.all(
            selectedConnectors.map(({ connect, name }) =>
              deleteMutation.mutateAsync({
                props: {
                  clusterName,
                  connectName: connect,
                  connectorName: name,
                },
              })
            )
          );
        } catch (error) {
          showServerError(error as Response);
        } finally {
          resetRowSelection();
        }
      }
    );
  };

  return (
    <>
      <ActionButton
        buttonType="secondary"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
        }}
        onClick={() =>
          performBatchConnectorsAction(
            'Are you sure you want to pause selected connectors?',
            ConnectorAction.PAUSE
          )
        }
      >
        Pause Connectors
      </ActionButton>
      <ActionButton
        buttonType="secondary"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
        }}
        onClick={() =>
          performBatchConnectorsAction(
            'Are you sure you want to resume selected connectors?',
            ConnectorAction.RESUME
          )
        }
      >
        Resume Connectors
      </ActionButton>
      <ActionButton
        buttonType="secondary"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
        }}
        onClick={() =>
          performBatchConnectorsAction(
            'Are you sure you want to restart selected connectors?',
            ConnectorAction.RESTART
          )
        }
      >
        Restart Connectors
      </ActionButton>
      <ActionButton
        buttonType="secondary"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
        }}
        onClick={() =>
          performBatchConnectorsAction(
            'Are you sure you want to restart all tasks in selected connectors?',
            ConnectorAction.RESTART_ALL_TASKS
          )
        }
      >
        Restart All Tasks
      </ActionButton>
      <ActionButton
        buttonType="secondary"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.RESTART,
        }}
        onClick={() =>
          performBatchConnectorsAction(
            'Are you sure you want to restart all failed tasks in selected connectors?',
            ConnectorAction.RESTART_FAILED_TASKS
          )
        }
      >
        Restart Failed Tasks
      </ActionButton>
      <ActionButton
        buttonType="danger"
        buttonSize="M"
        disabled={!rows.length}
        permission={{
          resource: ResourceType.CONNECT,
          action: Action.DELETE,
        }}
        onClick={deleteConnectorsFailedTasksHandler}
      >
        Remove Connectors
      </ActionButton>
    </>
  );
};

export default BatchActionsBar;
