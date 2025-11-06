import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import { MultiLineTag } from 'components/common/Tag/Tag.styled';
import { ClusterNameRoute, clusterTopicPath } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';

import * as S from './TopicsCell.styled';

const TopicsCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { topics } = row.original;
  const { clusterName } = useAppParams<ClusterNameRoute>();

  return (
    <S.TagsWrapper>
      {topics?.map((t) => {
        const href = clusterTopicPath(clusterName, t);

        return (
          <MultiLineTag key={t} color="green">
            <a href={href}>{t}</a>
          </MultiLineTag>
        );
      })}
    </S.TagsWrapper>
  );
};

export default TopicsCell;
