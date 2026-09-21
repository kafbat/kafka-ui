import React from 'react';
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
import ErrorPage from 'components/ErrorPage/ErrorPage';
import TableRefresh from 'components/common/TableRefresh/TableRefresh';
import { useRefreshRate } from 'lib/hooks/useRefreshRate';

import * as S from './ListPage.styled';
import { ConnectorsTable } from './ConnectorsTable/ConnectorsTable';
import ConnectorsStatistics from './Statistics/Statistics';

const emptyConnectors: FullConnectorInfo[] = [];

const ListPage: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('connects');
  const { refetchInterval } = useRefreshRate('connectors-refresh-rate');
  const {
    data: connectors = emptyConnectors,
    isLoading,
    isSuccess,
    isFetching,
    error,
    refetch,
  } = useConnectors(clusterName, searchParams.get('q') || '', isFtsEnabled, {
    refetchInterval,
    refetchIntervalInBackground: false,
  });

  return (
    <FilteredConnectorsProvider>
      <ConnectorsStatistics isLoading={isLoading} />
      <S.Search hasInput>
        <Search
          key={clusterName}
          placeholder="Search by Connect Name, Status or Type"
          extraActions={<Fts resourceName="connects" />}
        />
        <TableRefresh
          storageKey="connectors-refresh-rate"
          onRefresh={refetch}
          isFetching={isFetching}
        />
      </S.Search>

      {isLoading && <PageLoader offsetY={370} />}

      {error && (
        <ErrorPage
          offsetY={370}
          status={error.status}
          onClick={refetch}
          text={error.message}
        />
      )}

      {isSuccess && <ConnectorsTable connectors={connectors} />}
    </FilteredConnectorsProvider>
  );
};

export default ListPage;
