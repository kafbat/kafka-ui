import React from 'react';
import { CellContext } from '@tanstack/react-table';
import { Link } from 'react-router-dom';
import { ClusterGroupParam, clusterTopicPath } from 'lib/paths';
import { TableKeyLink } from 'components/common/table/Table/TableKeyLink.styled';
import useAppParams from 'lib/hooks/useAppParams';
import { ConsumerGroupTopicsTableRow } from 'components/ConsumerGroups/Details/TopicsTable/lib/types';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import { Action, ResourceType } from 'generated-sources';
import { Dropdown } from 'components/common/Dropdown';
import { useDeleteConsumerGroupOffsetsMutation } from 'lib/hooks/api/consumers';

type TopicNameProps = CellContext<
  ConsumerGroupTopicsTableRow,
  ConsumerGroupTopicsTableRow['topicName']
>;

export const TopicName = ({ getValue }: TopicNameProps) => {
  const routeParams = useAppParams<ClusterGroupParam>();
  const { clusterName } = routeParams;
  const topicName = getValue();

  return (
    <TableKeyLink style={{ width: 'fit-content' }}>
      <Link to={clusterTopicPath(clusterName, topicName)}>{topicName}</Link>
    </TableKeyLink>
  );
};

type ConsumerLagProps = CellContext<
  ConsumerGroupTopicsTableRow,
  ConsumerGroupTopicsTableRow['consumerLag']
>;

export const ConsumerLag = ({ getValue }: ConsumerLagProps) => getValue();

type ActionsProps = CellContext<
  ConsumerGroupTopicsTableRow,
  ConsumerGroupTopicsTableRow['topicName']
>;

export const Actions = ({ getValue }: ActionsProps) => {
  const routeParams = useAppParams<ClusterGroupParam>();
  const topicName = getValue();

  const deleteOffsetMutation =
    useDeleteConsumerGroupOffsetsMutation(routeParams);

  const deleteOffsetHandler = () => {
    if (topicName === undefined) return;
    deleteOffsetMutation.mutateAsync(topicName);
  };

  return (
    <Dropdown>
      <ActionDropdownItem
        onClick={deleteOffsetHandler}
        danger
        confirm="Are you sure you want to delete offsets from the topic?"
        permission={{
          resource: ResourceType.CONSUMER,
          action: Action.RESET_OFFSETS,
          value: routeParams.consumerGroupID,
        }}
      >
        <span>Delete offsets</span>
      </ActionDropdownItem>
    </Dropdown>
  );
};
