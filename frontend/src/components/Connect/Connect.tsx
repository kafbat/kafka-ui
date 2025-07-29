import React from 'react';
import { Navigate, Routes, Route, NavLink } from 'react-router-dom';
import {
  RouteParams,
  clusterConnectConnectorRelativePath,
  clusterConnectConnectorsRelativePath,
  clusterConnectorNewRelativePath,
  getNonExactPath,
  clusterConnectorsPath,
  ClusterNameRoute,
  kafkaConnectClustersPath,
  kafkaConnectClustersRelativePath,
  clusterConnectorsRelativePath,
} from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import SuspenseQueryComponent from 'components/common/SuspenseQueryComponent/SuspenseQueryComponent';
import Navbar from 'components/common/Navigation/Navbar.styled';
import Clusters from 'components/Connect/Clusters/Clusters';

import DetailsPage from './Details/DetailsPage';
import New from './New/New';
import Connectors from './List/ListPage';
import Header from './Header/Header';

const Connect: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();

  return (
    <>
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
    </>
  );
};

const ConnectOld: React.FC = () => {
  const { clusterName } = useAppParams();

  return (
    <Routes>
      <Route index element={<Connectors />} />
      <Route path={clusterConnectorNewRelativePath} element={<New />} />
      <Route
        path={getNonExactPath(clusterConnectConnectorRelativePath)}
        element={
          <SuspenseQueryComponent>
            <DetailsPage />
          </SuspenseQueryComponent>
        }
      />
      <Route
        path={clusterConnectConnectorsRelativePath}
        element={<Navigate to={clusterConnectorsPath(clusterName)} replace />}
      />
      <Route
        path={RouteParams.connectName}
        element={<Navigate to="/" replace />}
      />
    </Routes>
  );
};

export default Connect;
