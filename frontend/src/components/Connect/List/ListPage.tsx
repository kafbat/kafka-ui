import React, { Suspense } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Search from 'components/common/Search/Search';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import Fts from 'components/common/Fts/Fts';
import useFts from 'components/common/Fts/useFts';
import { useSearchParams } from 'react-router-dom';

import * as S from './ListPage.styled';
import List from './List';
import ConnectorsStatistics from './Statistics/Statistics';

const ListPage: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { isFtsEnabled } = useFts('connects');
  const [searchParams] = useSearchParams();
  const { data, isLoading } = useConnectors(
    clusterName,
    searchParams.get('q') || '',
    isFtsEnabled
  );

  return (
    <>
      <ConnectorsStatistics connectors={data ?? []} isLoading={isLoading} />
      <S.Search hasInput>
        <Search
          placeholder="Search by Connect Name, Status or Type"
          extraActions={<Fts resourceName="connects" />}
        />
      </S.Search>
      <Suspense fallback={<PageLoader />}>
        <List connectors={data ?? []} />
      </Suspense>
    </>
  );
};

export default ListPage;
