import React, { useState, Suspense } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute, clusterConnectorNewRelativePath } from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import Search from 'components/common/Search/Search';
import * as Metrics from 'components/common/Metrics';
import PageHeading from 'components/common/PageHeading/PageHeading';
import Tooltip from 'components/common/Tooltip/Tooltip';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { ConnectorState, Action, ResourceType } from 'generated-sources';
import { useConnectors, useConnects } from 'lib/hooks/api/kafkaConnect';
import { ActionButton } from 'components/common/ActionComponent';
import * as S from './ListPage.styled';

import List from './List';
import ClustersList from './ClustersList';

const ListPage: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();

  const { data: connects = [] } = useConnects(clusterName);

  // Fetches all connectors from the API, without search criteria. Used to display general metrics.
  const { data: connectorsMetrics, isLoading } = useConnectors(clusterName);
  const [viewType, setViewType] = useState<string>('connectors');

  const numberOfFailedConnectors = connectorsMetrics?.filter(
    ({ status: { state } }) => state === ConnectorState.FAILED
  ).length;

  const numberOfFailedTasks = connectorsMetrics?.reduce(
    (acc, metric) => acc + (metric.failedTasksCount ?? 0),
    0
  );

  return (
    <>
      <PageHeading text="Connectors">
        {!isReadOnly && (
          <Tooltip
            value={
              <ActionButton
                buttonType="primary"
                buttonSize="M"
                disabled={!connects.length}
                to={clusterConnectorNewRelativePath}
                permission={{
                  resource: ResourceType.CONNECT,
                  action: Action.CREATE,
                }}
              >
                Create Connector
              </ActionButton>
            }
            showTooltip={!connects.length}
            content="No Connects available"
            placement="left"
          />
        )}
      </PageHeading>
      <Metrics.Wrapper>
        <Metrics.Section>
          <Metrics.Indicator
            label="Connectors"
            title="Total number of connectors"
            fetching={isLoading}
          >
            {connectorsMetrics?.length || '-'}
          </Metrics.Indicator>
          <Metrics.Indicator
            label="Failed Connectors"
            title="Number of failed connectors"
            fetching={isLoading}
          >
            {numberOfFailedConnectors ?? '-'}
          </Metrics.Indicator>
          <Metrics.Indicator
            label="Failed Tasks"
            title="Number of failed tasks"
            fetching={isLoading}
          >
            {numberOfFailedTasks ?? '-'}
          </Metrics.Indicator>
          <Metrics.Indicator
            label="Clusters"
            title="Total number of clusters"
            fetching={isLoading}
          >
            {connects?.length || '-'}
          </Metrics.Indicator>
        </Metrics.Section>
      </Metrics.Wrapper>
      <ControlPanelWrapper hasInput>
        <Search placeholder="Search by Connect Name, Status or Type" />
      </ControlPanelWrapper>
      <S.Navbar>
        <S.Tab
          isActive={viewType === 'connectors'}
          onClick={() => setViewType('connectors')}
        >
          Connectors
        </S.Tab>
        <S.Tab
          isActive={viewType === 'clusters'}
          onClick={() => setViewType('clusters')}
        >
          Clusters
        </S.Tab>
      </S.Navbar>
      <Suspense fallback={<PageLoader />}>
        {viewType === 'connectors' && (
          <List />
        )}
        {viewType === 'clusters' && (
          <ClustersList />
        )}
      </Suspense>
    </>
  );
};

export default ListPage;
