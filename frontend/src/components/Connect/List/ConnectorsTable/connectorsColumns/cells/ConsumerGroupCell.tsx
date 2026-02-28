import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';
import { Link, useParams } from 'react-router-dom';
import { clusterConsumerGroupDetailsPath } from 'lib/paths';

const ConsumerGroupCell = ({ row }: CellContext<FullConnectorInfo, string>) => {
  const { consumer } = row.original;
  const { clusterName } = useParams<{ clusterName: string }>();

  if (!consumer || !clusterName) {
    return <span>-</span>;
  }

  return (
    <Link
      to={clusterConsumerGroupDetailsPath(
        clusterName,
        encodeURIComponent(consumer)
      )}
      style={{ color: 'var(--color-primary)' }}
    >
      {consumer}
    </Link>
  );
};

export default ConsumerGroupCell;
