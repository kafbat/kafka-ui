import React from 'react';
import Query from 'components/KsqlDb/Query/Query';
import useAppParams from 'lib/hooks/useAppParams';
import * as Metrics from 'components/common/Metrics';
import {
  clusterKsqlDbQueryRelativePath,
  clusterKsqlDbStreamsPath,
  clusterKsqlDbStreamsRelativePath,
  clusterKsqlDbTablesPath,
  clusterKsqlDbTablesRelativePath,
  ClusterNameRoute,
} from 'lib/paths';
import { ActionButton } from 'components/common/ActionComponent';
import Navbar from 'components/common/Navigation/Navbar.styled';
import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { Action, ResourceType } from 'generated-sources';
import { useKsqlTables, useKsqlStreams } from 'lib/hooks/api/ksqlDb';
import 'ace-builds/src-noconflict/ace';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import PageLoader from 'components/common/PageLoader/PageLoader';
import ErrorPage from 'components/ErrorPage/ErrorPage';

import TableView from './TableView';

const KsqlDb: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();

  const tables = useKsqlTables('clusterName');
  const streams = useKsqlStreams('clusterName');

  const isSuccess = tables.isSuccess && streams.isSuccess;
  const isLoading =
    tables.isLoading ||
    tables.isRefetching ||
    streams.isLoading ||
    streams.isRefetching;
  const error = tables.error || streams.error;

  return (
    <>
      <ResourcePageHeading text="KSQL DB">
        <ActionButton
          to={clusterKsqlDbQueryRelativePath}
          buttonType="primary"
          buttonSize="M"
          permission={{
            resource: ResourceType.KSQL,
            action: Action.EXECUTE,
          }}
        >
          Execute KSQL Request
        </ActionButton>
      </ResourcePageHeading>

      {isSuccess && (
        <Metrics.Wrapper>
          <Metrics.Section>
            <Metrics.Indicator
              label="Tables"
              title="Tables"
              fetching={isLoading}
            >
              {tables.data?.length || 0}
            </Metrics.Indicator>
            <Metrics.Indicator
              label="Streams"
              title="Streams"
              fetching={isLoading}
            >
              {streams.data?.length || 0}
            </Metrics.Indicator>
          </Metrics.Section>
        </Metrics.Wrapper>
      )}

      <div>
        <Navbar role="navigation">
          <NavLink
            to={clusterKsqlDbTablesPath(clusterName)}
            className={({ isActive }) => (isActive ? 'is-active' : '')}
            end
          >
            Tables
          </NavLink>
          <NavLink
            to={clusterKsqlDbStreamsPath(clusterName)}
            className={({ isActive }) => (isActive ? 'is-active' : '')}
            end
          >
            Streams
          </NavLink>
        </Navbar>

        {isLoading && <PageLoader offsetY={300} />}

        {error && (
          <ErrorPage
            offsetY={300}
            status={error.status}
            onClick={() => {
              tables.refetch();
              streams.refetch();
            }}
            text={error.message}
          />
        )}

        {isSuccess && (
          <Routes>
            <Route
              index
              element={<Navigate to={clusterKsqlDbTablesRelativePath} />}
            />
            <Route
              path={clusterKsqlDbTablesRelativePath}
              element={<TableView fetching={false} rows={tables.data || []} />}
            />
            <Route
              path={clusterKsqlDbStreamsRelativePath}
              element={<TableView fetching={false} rows={streams.data || []} />}
            />
            <Route path={clusterKsqlDbQueryRelativePath} element={<Query />} />
          </Routes>
        )}
      </div>
    </>
  );
};

export default KsqlDb;
