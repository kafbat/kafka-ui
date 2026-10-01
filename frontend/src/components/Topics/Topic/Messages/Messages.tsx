import React from 'react';
import { useTopicMessages } from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';

import MessagesTable from './MessagesTable';
import Filters from './Filters/Filters';

/** Connects topic-message polling state to filters and the message table. */
const Messages: React.FC = () => {
  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const {
    messages,
    isFetching,
    fetchRequestId,
    keySerde,
    valueSerde,
    consumptionStats,
    phase,
    abortFetchData,
  } = useTopicMessages({
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
        messages={messages}
      />
      <MessagesTable
        messages={messages}
        isFetching={isFetching}
        fetchRequestId={fetchRequestId}
        keySerde={keySerde}
        valueSerde={valueSerde}
        bytesLimitReached={!!consumptionStats?.bytesLimitReached}
        blockedMessage={consumptionStats?.blockedMessage}
      />
    </>
  );
};

export default Messages;
