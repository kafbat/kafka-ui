import React from 'react';
import {Action, ConsumerGroupTopicPartition, ResourceType} from 'generated-sources';
import { Link } from 'react-router-dom';
import { ClusterName } from 'lib/interfaces/cluster';
import {ClusterGroupParam, clusterTopicPath} from 'lib/paths';
import MessageToggleIcon from 'components/common/Icons/MessageToggleIcon';
import IconButtonWrapper from 'components/common/Icons/IconButtonWrapper';
import { TableKeyLink } from 'components/common/table/Table/TableKeyLink.styled';

import TopicContents from './TopicContents/TopicContents';
import { FlexWrapper } from './ListItem.styled';
import {Dropdown} from "../../common/Dropdown";
import {ActionDropdownItem} from "../../common/ActionComponent";
import useAppParams from "../../../lib/hooks/useAppParams";
import {useDeleteConsumerGroupOffsetsMutation} from "../../../lib/hooks/api/consumers";

interface Props {
  clusterName: ClusterName;
  name: string;
  consumers: ConsumerGroupTopicPartition[];
}

const ListItem: React.FC<Props> = ({ clusterName, name, consumers }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const consumer = useAppParams<ClusterGroupParam>()
  const deleteOffset = useDeleteConsumerGroupOffsetsMutation(consumer)

  const getTotalconsumerLag = () => {
    let count = 0;
    consumers.forEach((consumer) => {
      count += consumer?.consumerLag || 0;
    });
    return count;
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
        <td>
          {getTotalconsumerLag()}
        </td>
        <td>
          <Dropdown>
            <ActionDropdownItem
              onClick={() => deleteOffset.mutateAsync(name)}
              permission={{
                resource: ResourceType.CONSUMER,
                action: Action.DELETE_OFFSETS,
                value: consumer.consumerGroupID
              }}>
              Unsubscribe from topic
            </ActionDropdownItem>
          </Dropdown>
        </td>
      </tr>
      {isOpen && <TopicContents consumers={consumers} />}
    </>
  );
};

export default ListItem;
