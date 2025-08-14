import AlertBadge from 'components/common/AlertBadge/AlertBadge';
import { Connect } from 'generated-sources';
import React from 'react';

type Props = { connect: Connect };
const TasksCell = ({ connect }: Props) => {
  const count = connect.tasksCount ?? 0;
  const failedCount = connect.failedTasksCount ?? 0;
  const text = `${count - failedCount}/${count}`;

  if (!count) {
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

  return <div>{text}</div>;
};
export default TasksCell;
