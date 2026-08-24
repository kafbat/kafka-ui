import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { Tag } from 'components/common/Tag/Tag.styled';
import { Cluster } from 'generated-sources';

type ClusterNameProps = CellContext<Cluster, unknown>;

const ClusterName: React.FC<ClusterNameProps> = ({ row }) => {
  const { readOnly, name } = row.original;
  return (
    <div style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>
      {readOnly && (
        <Tag color="blue" style={{ marginRight: '0.75em' }}>
          readonly
        </Tag>
      )}
      {name}
    </div>
  );
};

export default ClusterName;
