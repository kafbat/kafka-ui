import React, { Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import PageLoader from 'components/common/PageLoader/PageLoader';
import {
  clusterTopicCopyRelativePath,
  clusterTopicNewRelativePath,
  getNonExactPath,
  RouteParams,
} from 'lib/paths';
import SuspenseQueryComponent from 'components/common/SuspenseQueryComponent/SuspenseQueryComponent';

const New = React.lazy(() => import('./New/New'));
const ListPage = React.lazy(() => import('./List/ListPage'));
const Topic = React.lazy(() => import('./Topic/Topic'));

const Topics: React.FC = () => (
  <Suspense fallback={<PageLoader />}>
    <Routes>
      <Route index element={<ListPage />} />
      <Route path={clusterTopicNewRelativePath} element={<New />} />
      <Route path={clusterTopicCopyRelativePath} element={<New />} />
      <Route
        path={getNonExactPath(RouteParams.topicName)}
        element={
          <SuspenseQueryComponent>
            <Topic />
          </SuspenseQueryComponent>
        }
      />
    </Routes>
  </Suspense>
);

export default Topics;
