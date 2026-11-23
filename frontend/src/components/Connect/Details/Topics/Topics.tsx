import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { RouterParamsClusterConnectConnector } from 'lib/paths';
import { useConnector } from 'lib/hooks/api/kafkaConnect';
import Table from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';
import { Button } from 'components/common/Button/Button';

import { TopicNameCell } from './cells/TopicNameCell';
import * as S from './Topics.styled';

const columns: ColumnDef<{ topicName: string }>[] = [
  {
    header: 'Topic',
    accessorKey: 'topicName',
    cell: TopicNameCell,
  },
];

export const COLLAPSED_TOPICS_COUNT = 10;

const Topics = () => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const [isExpanded, setIsExpanded] = React.useState(false);

  const { data: connector } = useConnector(routerProps);

  const topics = connector?.topics ?? [];
  const visibleTopics = isExpanded
    ? topics
    : topics.slice(0, COLLAPSED_TOPICS_COUNT);
  const tableData = visibleTopics.map((topicName) => ({ topicName }));

  return (
    <>
      <Table
        columns={columns}
        data={tableData}
        emptyMessage="No topics found"
      />
      {topics.length > COLLAPSED_TOPICS_COUNT && (
        <S.ToggleButtonWrapper>
          <Button
            buttonType="text"
            buttonSize="S"
            onClick={() => setIsExpanded((prevState) => !prevState)}
          >
            {isExpanded ? 'Show less' : `Show all ${topics.length} topics`}
          </Button>
        </S.ToggleButtonWrapper>
      )}
    </>
  );
};

export default Topics;
