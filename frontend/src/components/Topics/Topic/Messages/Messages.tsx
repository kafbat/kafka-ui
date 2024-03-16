import React from 'react';
import { useTopicMessages } from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';

import MessagesTable from './MessagesTable';
import Filters from './Filters/Filters';

const Messages: React.FC = () => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const { messages, isFetching, consumptionStats, phase, abortFetchData } =
    useTopicMessages({
      clusterName,
      topicName,
    });

  return (
    <>
      <Filters
        consumptionStats={consumptionStats}
        isFetching={isFetching}
        phaseMessage={phase}
        abortFetchData={abortFetchData}
      />
      <MessagesTable messages={messages} isFetching={isFetching} />
    </>
  );
};

export default Messages;
