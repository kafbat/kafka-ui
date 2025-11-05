import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { ConnectorsTable } from 'components/Connect/List/ConnectorsTable/ConnectorsTable';

type ConnectorsProps = {
  connectors: FullConnectorInfo[];
};

const Connectors: React.FC<ConnectorsProps> = ({ connectors }) => (
  <ConnectorsTable
    columnVisibility={{ topics: false }}
    connectors={connectors}
    columnSizingPersistKey="topic-connectors"
  />
);

export default Connectors;
