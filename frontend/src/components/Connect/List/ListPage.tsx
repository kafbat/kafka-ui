import React, { Suspense } from 'react';
import Search from 'components/common/Search/Search';
import PageLoader from 'components/common/PageLoader/PageLoader';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { useSearchParams } from 'react-router-dom';
import useFts from 'components/common/Fts/useFts';
import Fts from 'components/common/Fts/Fts';
import { FullConnectorInfo } from 'generated-sources';
import { FilteredConnectorsProvider } from 'components/Connect/model/FilteredConnectorsProvider';

import * as S from './ListPage.styled';
import List from './List';
import ConnectorsStatistics from './Statistics/Statistics';

const emptyConnectors: FullConnectorInfo[] = [];

const ListPage: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('connects');
  const { data: connectors = emptyConnectors, isLoading } = useConnectors(
    clusterName,
    searchParams.get('q') || '',
    isFtsEnabled
  );

  return (
    <FilteredConnectorsProvider>
      <ConnectorsStatistics isLoading={isLoading} />
      <S.Search hasInput>
        <Search
          placeholder="Search by Connect Name, Status or Type"
          extraActions={<Fts resourceName="connects" />}
        />
      </S.Search>
      <Suspense fallback={<PageLoader />}>
        <List connectors={connectors} />
      </Suspense>
    </FilteredConnectorsProvider>
  );
};

export default ListPage;
