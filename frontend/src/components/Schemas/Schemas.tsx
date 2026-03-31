import React, { Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import {
  clusterSchemaEditRelativePath,
  clusterSchemaNewRelativePath,
  clusterSchemaSchemaDiffRelativePath,
  RouteParams,
} from 'lib/paths';
import Details from 'components/Schemas/Details/Details';
import New from 'components/Schemas/New/New';
import Edit from 'components/Schemas/Edit/Edit';
import Diff from 'components/Schemas/Diff/Diff';
import PageLoader from 'components/common/PageLoader/PageLoader';

const List = React.lazy(() => import('./List/List'));

const Schemas: React.FC = () => {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route index element={<List />} />
        <Route path={clusterSchemaNewRelativePath} element={<New />} />
        <Route path={RouteParams.subject} element={<Details />} />
        <Route path={clusterSchemaEditRelativePath} element={<Edit />} />
        <Route path={clusterSchemaSchemaDiffRelativePath} element={<Diff />} />
      </Routes>
    </Suspense>
  );
};

export default Schemas;
