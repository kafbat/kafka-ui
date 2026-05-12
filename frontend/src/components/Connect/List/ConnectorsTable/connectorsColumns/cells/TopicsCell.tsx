import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import { MultiLineTag } from 'components/common/Tag/Tag.styled';
import { ClusterNameRoute, clusterTopicPath } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { Button } from 'components/common/Button/Button';

import * as S from './TopicsCell.styled';

const COLLAPSED_TOPICS_COUNT = 5;

const TopicsCell: React.FC<CellContext<FullConnectorInfo, unknown>> = ({
  row,
}) => {
  const { topics } = row.original;
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [isExpanded, setIsExpanded] = React.useState(false);

  const visibleTopics = isExpanded
    ? topics
    : topics?.slice(0, COLLAPSED_TOPICS_COUNT);

  const hiddenTopicsCount = Math.max(
    0,
    (topics?.length ?? 0) - COLLAPSED_TOPICS_COUNT
  );

  return (
    <>
      <S.TagsWrapper>
        {visibleTopics?.map((t) => {
          const href = clusterTopicPath(clusterName, t);

          return (
            <MultiLineTag key={t} color="green">
              <a href={href}>{t}</a>
            </MultiLineTag>
          );
        })}
      </S.TagsWrapper>
      {hiddenTopicsCount > 0 && (
        <S.ToggleButtonWrapper>
          <Button
            buttonType="text"
            buttonSize="S"
            onClick={() => setIsExpanded((prevState) => !prevState)}
          >
            {isExpanded ? 'Show less' : `Show ${hiddenTopicsCount} more`}
          </Button>
        </S.ToggleButtonWrapper>
      )}
    </>
  );
};

export default TopicsCell;
