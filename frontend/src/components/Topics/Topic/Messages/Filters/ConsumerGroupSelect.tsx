import React, { useMemo } from 'react';
import Select from 'components/common/Select/Select';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import { useTopicConsumerGroups } from 'lib/hooks/api/topics';

export interface ConsumerGroupSelectProps {
  value?: string;
  onChange: (consumerGroupId: string) => void;
}

const ConsumerGroupSelect: React.FC<ConsumerGroupSelectProps> = ({
  value,
  onChange,
}) => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { data: consumerGroups = [] } = useTopicConsumerGroups({
    clusterName,
    topicName,
  });

  const options = useMemo(
    () =>
      consumerGroups.map(({ groupId }) => ({ label: groupId, value: groupId })),
    [consumerGroups]
  );

  return (
    <Select
      id="selectConsumerGroup"
      aria-labelledby="selectConsumerGroup"
      onChange={onChange}
      options={options}
      value={value}
      minWidth="200px"
      selectSize="M"
      disabled={options.length === 0}
      placeholder={
        options.length === 0 ? 'No consumer groups' : 'Select consumer group'
      }
    />
  );
};

export default ConsumerGroupSelect;
