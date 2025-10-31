import React from 'react';

import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { useSearchParams } from 'react-router-dom';
import { ConnectorsTable } from 'components/Connect/List/ConnectorsTable/ConnectorsTable';

const List: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { data: connectors = [] } = useConnectors(
    clusterName,
    searchParams.get('q') || ''
  );

  return <ConnectorsTable connectors={connectors} />;
};

export default List;
