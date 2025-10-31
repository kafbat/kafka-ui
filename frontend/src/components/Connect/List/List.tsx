import React from 'react';
import { ConnectorsTable } from 'components/Connect/List/ConnectorsTable/ConnectorsTable';
import { FullConnectorInfo } from 'generated-sources';

interface ConnectorsListProps {
  connectors: FullConnectorInfo[];
}

const List: React.FC<ConnectorsListProps> = ({ connectors }) => {
  return <ConnectorsTable connectors={connectors} />;
};

export default List;
