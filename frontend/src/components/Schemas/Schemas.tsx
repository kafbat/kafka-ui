import React from 'react';
import { Route, Routes } from 'react-router-dom';
import {
  clusterSchemaEditRelativePath,
  clusterSchemaNewRelativePath,
  clusterSchemaSchemaDiffRelativePath,
  RouteParams,
} from 'lib/paths';
import List from 'components/Schemas/List/List';
import Details from 'components/Schemas/Details/Details';
import New from 'components/Schemas/New/New';
import Edit from 'components/Schemas/Edit/Edit';
import Diff from 'components/Schemas/Diff/Diff';

const Schemas: React.FC = () => {
  return (
    <Routes>
      <Route index element={<List />} />
      <Route path={clusterSchemaNewRelativePath} element={<New />} />
      <Route path={RouteParams.subject} element={<Details />} />
      <Route path={clusterSchemaEditRelativePath} element={<Edit />} />
      <Route path={clusterSchemaSchemaDiffRelativePath} element={<Diff />} />
    </Routes>
  );
};

export default Schemas;
