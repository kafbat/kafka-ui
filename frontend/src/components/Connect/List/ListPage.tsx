import React, { Suspense } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Search from 'components/common/Search/Search';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';

import * as S from './ListPage.styled';
import List from './List';
import ConnectorsStatistics from './Statistics/Statistics';

const ListPage: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data, isLoading } = useConnectors(clusterName);

  return (
    <>
      <ConnectorsStatistics connectors={data ?? []} isLoading={isLoading} />
      <S.Search hasInput>
        <Search placeholder="Search by Connect Name, Status or Type" />
      </S.Search>
      <Suspense fallback={<PageLoader />}>
        <List />
      </Suspense>
    </>
  );
};

export default ListPage;
