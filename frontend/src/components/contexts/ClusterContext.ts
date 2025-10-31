import React from 'react';

export interface ContextProps {
  isReadOnly: boolean;
  hasKafkaConnectConfigured: boolean;
  hasSchemaRegistryConfigured: boolean;
  isTopicDeletionAllowed: boolean;
  ftsEnabled: boolean;
  ftsDefaultEnabled: boolean;
}

export const initialValue: ContextProps = {
  isReadOnly: false,
  hasKafkaConnectConfigured: false,
  hasSchemaRegistryConfigured: false,
  isTopicDeletionAllowed: true,
  ftsEnabled: false,
  ftsDefaultEnabled: false,
};
const ClusterContext = React.createContext(initialValue);

export default ClusterContext;
