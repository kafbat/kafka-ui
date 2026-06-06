import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { Tag } from 'components/common/Tag/Tag.styled';
import { Cluster } from 'generated-sources';

import * as S from './Dashboard.styled';

type ClusterNameProps = CellContext<Cluster, unknown>;

const ClusterName: React.FC<ClusterNameProps> = ({ row }) => {
  const { readOnly, name } = row.original;
  return (
    <S.ClusterNameCell>
      {readOnly && <Tag color="blue">readonly</Tag>}
      {name}
    </S.ClusterNameCell>
  );
};

export default ClusterName;
