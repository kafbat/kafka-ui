import AlertBadge from 'components/common/AlertBadge/AlertBadge';
import { Connect } from 'generated-sources';
import React from 'react';

export const getConnectorsCountText = (connect: Connect) => {
  const count = connect.connectorsCount ?? 0;
  const failedCount = connect.failedConnectorsCount ?? 0;

  return {
    count,
    failedCount,
    text: `${count - failedCount}/${count}`,
  };
};

type Props = { connect: Connect };

const ConnectorsCell = ({ connect }: Props) => {
  const { count, failedCount, text } = getConnectorsCountText(connect);

  if (count === 0) return null;

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
export default ConnectorsCell;
