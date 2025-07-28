import AlertBadge from 'components/common/AlertBadge/AlertBadge';
import { Connect } from 'generated-sources';
import React from 'react';

type Props = { connect: Connect };
const ConnectorsCell = ({ connect }: Props) => {
  const count = connect.connectorsCount ?? 0;
  const failedCount = connect.failedConnectorsCount ?? 0;
  const text = `${count - failedCount}/${count}`;

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
