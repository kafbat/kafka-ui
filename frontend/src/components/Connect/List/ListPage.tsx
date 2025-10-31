import React, { Suspense } from 'react';
import Search from 'components/common/Search/Search';
import PageLoader from 'components/common/PageLoader/PageLoader';

import * as S from './ListPage.styled';
import List from './List';
import ConnectorsStatistics from './Statistics/Statistics';

const ListPage: React.FC = () => (
  <>
    <ConnectorsStatistics />
    <S.Search hasInput>
      <Search placeholder="Search by Connect Name, Status or Type" />
    </S.Search>
    <Suspense fallback={<PageLoader />}>
      <List />
    </Suspense>
  </>
);

export default ListPage;
