import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import AlertBadge from 'components/common/AlertBadge/AlertBadge';

const RunningTasksCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { tasksCount, failedTasksCount } = row.original;

  const failedCount = failedTasksCount ?? 0;
  const count = tasksCount ?? 0;

  const text = `${count - failedCount}/${count}`;

  if (!tasksCount) {
    return null;
  }

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
