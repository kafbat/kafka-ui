import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import AlertBadge from 'components/common/AlertBadge/AlertBadge';

export const getRunningTasksCountText = (connector: FullConnectorInfo) => {
  const { tasksCount, failedTasksCount } = connector;

  const failedCount = failedTasksCount ?? 0;
  const count = tasksCount ?? 0;

  const text = `${count - failedCount}/${count}`;

  return { count, failedCount, text };
};

const RunningTasksCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { count, failedCount, text } = getRunningTasksCountText(row.original);

  if (!count) return null;

  if (failedCount > 0) {
    return (
      <AlertBadge>
        <AlertBadge.Content content={text} />
        <AlertBadge.Icon />
      </AlertBadge>
    );
  }

  return text;
};

export default RunningTasksCell;
