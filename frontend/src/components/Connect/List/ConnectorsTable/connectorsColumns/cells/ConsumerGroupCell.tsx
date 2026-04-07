import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { FullConnectorInfo } from 'generated-sources';
import { Link, useParams } from 'react-router-dom';
import { clusterConsumerGroupDetailsPath } from 'lib/paths';
import { MultiLineChipTag } from 'components/common/Tag/Tag.styled';

const ConsumerGroupCell = ({ row }: CellContext<FullConnectorInfo, string>) => {
  const { consumer } = row.original;
  const { clusterName } = useParams<{ clusterName: string }>();

  if (!consumer || !clusterName) {
    return <span>-</span>;
  }

  const toConsumerGroupDetails = clusterConsumerGroupDetailsPath(
    clusterName,
    consumer
  );

  return (
    <MultiLineChipTag color="gray">
      <Link to={toConsumerGroupDetails}>{consumer}</Link>
    </MultiLineChipTag>
  );
};

export default ConsumerGroupCell;
