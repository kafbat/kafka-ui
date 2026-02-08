import React, { Suspense } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterConnectConnectorConfigPath,
  clusterConnectConnectorConfigRelativePath,
  clusterConnectConnectorPath,
  clusterConnectConnectorTopicsPath,
  clusterConnectConnectorTopicsRelativePath,
  clusterConnectorsPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import Navbar from 'components/common/Navigation/Navbar.styled';
import PageLoader from 'components/common/PageLoader/PageLoader';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import { useConnector, useConnectorTasks } from 'lib/hooks/api/kafkaConnect';
import ErrorPage from 'components/ErrorPage/ErrorPage';

import Overview from './Overview/Overview';
import Tasks from './Tasks/Tasks';
import Config from './Config/Config';
import Actions from './Actions/Actions';
import Topics from './Topics/Topics';

const DetailsPage: React.FC = () => {
  const { clusterName, connectName, connectorName } =
    useAppParams<RouterParamsClusterConnectConnector>();

  const connector = useConnector({ clusterName, connectName, connectorName });
  const tasks = useConnectorTasks({ clusterName, connectName, connectorName });

  const isSuccess = connector.isSuccess && tasks.isSuccess;
  const isLoading =
    connector.isLoading ||
    connector.isRefetching ||
    tasks.isLoading ||
    tasks.isRefetching;
  const error = connector.error || tasks.error;

  return (
    <div>
      <ResourcePageHeading
        text={connectorName}
        backTo={clusterConnectorsPath(clusterName)}
        backText="Connectors"
      >
        <Actions />
      </ResourcePageHeading>
      {isSuccess && <Overview tasks={tasks.data} connector={connector.data} />}
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
        <NavLink
          to={clusterConnectConnectorTopicsPath(
            clusterName,
            connectName,
            connectorName
          )}
          className={({ isActive }) => (isActive ? 'is-active' : '')}
        >
          Topics
        </NavLink>
      </Navbar>

      {isLoading && <PageLoader offsetY={200} />}

      {error && (
        <ErrorPage
          offsetY={200}
          status={error.status}
          onClick={() => {
            connector.refetch();
            tasks.refetch();
          }}
          resourceName={`Connector ${connectorName}`}
        />
      )}

      {isSuccess && (
        <Routes>
          <Route index element={<Tasks />} />
          <Route
            path={clusterConnectConnectorConfigRelativePath}
            element={<Config />}
          />
          <Route
            path={clusterConnectConnectorTopicsRelativePath}
            element={<Topics />}
          />
        </Routes>
      )}
    </div>
  );
};

export default DetailsPage;
