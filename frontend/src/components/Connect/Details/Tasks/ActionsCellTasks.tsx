import React from 'react';
import { Action, ResourceType, Task } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import useAppParams from 'lib/hooks/useAppParams';
import { useRestartConnectorTask } from 'lib/hooks/api/kafkaConnect';
import { Dropdown } from 'components/common/Dropdown';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import { RouterParamsClusterConnectConnector } from 'lib/paths';

const ActionsCellTasks: React.FC<CellContext<Task, unknown>> = ({ row }) => {
  const { id } = row.original;
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const restartMutation = useRestartConnectorTask(routerProps);

  const restartTaskHandler = (taskId?: number) => {
    if (taskId === undefined) return;
    restartMutation.mutateAsync(taskId);
  };

  const connectorPath = `${routerProps.connectName}/${routerProps.connectorName}`;

  return (
    <Dropdown>
      <ActionDropdownItem
        onClick={() => restartTaskHandler(id?.task)}
        danger
        confirm="Are you sure you want to restart the task?"
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
        <span>Restart task</span>
      </ActionDropdownItem>
    </Dropdown>
  );
};

export default ActionsCellTasks;
