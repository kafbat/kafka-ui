import React from 'react';
import { FullConnectorInfo } from 'generated-sources';
import { ConnectorsTable } from 'components/Connect/List/ConnectorsTable/ConnectorsTable';
import { FilteredConnectorsProvider } from 'components/Connect/model/FilteredConnectorsProvider';

type ConnectorsProps = {
  connectors: FullConnectorInfo[];
};

const Connectors: React.FC<ConnectorsProps> = ({ connectors }) => (
  <FilteredConnectorsProvider>
    <ConnectorsTable
      columnVisibility={{ topics: false }}
      connectors={connectors}
      columnSizingPersistKey="topic-connectors"
    />
  </FilteredConnectorsProvider>
);

export default Connectors;
