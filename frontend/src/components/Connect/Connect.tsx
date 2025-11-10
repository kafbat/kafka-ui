import React from 'react';
import { Navigate, Routes, Route, NavLink } from 'react-router-dom';
import {
  clusterConnectorsPath,
  ClusterNameRoute,
  kafkaConnectClustersPath,
  kafkaConnectClustersRelativePath,
  clusterConnectorsRelativePath,
} from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import Navbar from 'components/common/Navigation/Navbar.styled';
import Clusters from 'components/Connect/Clusters/Clusters';
import { TableProvider } from 'components/common/NewTable';

import Connectors from './List/ListPage';
import Header from './Header/Header';

const Connect: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();

  return (
    <TableProvider>
      <Header />
      <Navbar role="navigation">
        <NavLink
          to={kafkaConnectClustersPath(clusterName)}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          end
        >
          Clusters
        </NavLink>
        <NavLink
          to={clusterConnectorsPath(clusterName)}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          end
        >
          Connectors
        </NavLink>
      </Navbar>
      <Routes>
        <Route
          index
          element={
            <Navigate to={kafkaConnectClustersPath(clusterName)} replace />
          }
        />
        <Route path={kafkaConnectClustersRelativePath} element={<Clusters />} />
        <Route path={clusterConnectorsRelativePath} element={<Connectors />} />
      </Routes>
    </TableProvider>
  );
};

export default Connect;
