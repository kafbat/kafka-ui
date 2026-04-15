import React from 'react';

export interface ContextProps {
  isReadOnly: boolean;
  hasKafkaConnectConfigured: boolean;
  hasSchemaRegistryConfigured: boolean;
  isTopicDeletionAllowed: boolean;
  ftsEnabled: boolean;
  ftsDefaultEnabled: boolean;
  messageRelativeTimestamp?: boolean;
  disableMessageViewing: boolean;
}

export const initialValue: ContextProps = {
  isReadOnly: false,
  hasKafkaConnectConfigured: false,
  hasSchemaRegistryConfigured: false,
  isTopicDeletionAllowed: true,
  ftsEnabled: false,
  ftsDefaultEnabled: false,
  messageRelativeTimestamp: false,
  disableMessageViewing: false,
};
const ClusterContext = React.createContext(initialValue);

export default ClusterContext;
