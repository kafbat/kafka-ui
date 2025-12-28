import AlertBadge from 'components/common/AlertBadge/AlertBadge';
import { Connect } from 'generated-sources';
import React from 'react';

export const getTasksCountText = (connect: Connect) => {
  const count = connect.tasksCount ?? 0;
  const failedCount = connect.failedTasksCount ?? 0;
  const text = `${count - failedCount}/${count}`;

  return { count, failedCount, text };
};

type Props = { connect: Connect };

const TasksCell = ({ connect }: Props) => {
  const { count, failedCount, text } = getTasksCountText(connect);

  if (!count) return null;

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
