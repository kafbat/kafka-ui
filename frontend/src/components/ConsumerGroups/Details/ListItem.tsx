import React from 'react';
import {
  Action,
  ConsumerGroupTopicPartition,
  ResourceType,
} from 'generated-sources';
import { Link } from 'react-router-dom';
import { ClusterName } from 'lib/interfaces/cluster';
import { ClusterGroupParam, clusterTopicPath } from 'lib/paths';
import { useDeleteConsumerGroupOffsetsMutation } from 'lib/hooks/api/consumers';
import useAppParams from 'lib/hooks/useAppParams';
import { Dropdown } from 'components/common/Dropdown';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import MessageToggleIcon from 'components/common/Icons/MessageToggleIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import { TableKeyLink } from 'components/common/table/Table/TableKeyLink.styled';

import TopicContents from './TopicContents/TopicContents';
import { FlexWrapper } from './ListItem.styled';

interface Props {
  clusterName: ClusterName;
  name: string;
  consumers: ConsumerGroupTopicPartition[];
}

const ListItem: React.FC<Props> = ({ clusterName, name, consumers }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const consumerProps = useAppParams<ClusterGroupParam>();
  const deleteOffsetMutation =
    useDeleteConsumerGroupOffsetsMutation(consumerProps);

  const getTotalconsumerLag = () => {
    if (consumers.every((consumer) => consumer?.consumerLag === null)) {
      return 'N/A';
    }
    let count = 0;
    consumers.forEach((consumer) => {
      count += consumer?.consumerLag || 0;
    });
    return count;
  };

  const deleteOffsetHandler = (topicName?: string) => {
    if (topicName === undefined) return;
    deleteOffsetMutation.mutateAsync(topicName);
  };

  return (
    <>
      <tr>
        <td>
          <FlexWrapper>
            <IconButtonWrapper onClick={() => setIsOpen(!isOpen)} aria-hidden>
              <MessageToggleIcon isOpen={isOpen} />
            </IconButtonWrapper>
            <TableKeyLink>
              <Link to={clusterTopicPath(clusterName, name)}>{name}</Link>
            </TableKeyLink>
          </FlexWrapper>
        </td>
        <td>{getTotalconsumerLag()}</td>
        <td>
          <Dropdown>
            <ActionDropdownItem
              onClick={() => deleteOffsetHandler(name)}
              danger
              confirm="Are you sure you want to delete offsets from the topic?"
              permission={{
                resource: ResourceType.CONSUMER,
                action: Action.RESET_OFFSETS,
                value: consumerProps.consumerGroupID,
              }}
            >
              <span>Delete offsets</span>
            </ActionDropdownItem>
          </Dropdown>
        </td>
      </tr>
      {isOpen && <TopicContents consumers={consumers} />}
    </>
  );
};

export default ListItem;
