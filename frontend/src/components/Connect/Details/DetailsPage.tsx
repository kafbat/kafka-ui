import React, { Suspense } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterConnectConnectorConfigPath,
  clusterConnectConnectorConfigRelativePath,
  clusterConnectConnectorPath,
  clusterConnectorsPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import Navbar from 'components/common/Navigation/Navbar.styled';
import PageLoader from 'components/common/PageLoader/PageLoader';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';

import Overview from './Overview/Overview';
import Tasks from './Tasks/Tasks';
import Config from './Config/Config';
import Actions from './Actions/Actions';

const DetailsPage: React.FC = () => {
  const { clusterName, connectName, connectorName } =
    useAppParams<RouterParamsClusterConnectConnector>();

  return (
    <div>
      <ResourcePageHeading
        text={connectorName}
        backTo={clusterConnectorsPath(clusterName)}
        backText="Connectors"
      >
        <Actions />
      </ResourcePageHeading>
      <Overview />
      <Navbar role="navigation">
        <NavLink
          to={clusterConnectConnectorPath(
            clusterName,
            connectName,
            connectorName
          )}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
          end
        >
          Tasks
        </NavLink>
        <NavLink
          to={clusterConnectConnectorConfigPath(
            clusterName,
            connectName,
            connectorName
          )}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
        >
          Config
        </NavLink>
      </Navbar>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route index element={<Tasks />} />
          <Route
            path={clusterConnectConnectorConfigRelativePath}
            element={<Config />}
          />
        </Routes>
      </Suspense>
    </div>
  );
};

export default DetailsPage;
