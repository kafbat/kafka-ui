import React, { Suspense } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectorNewRelativePath, ClusterNameRoute } from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import Search from 'components/common/Search/Search';
import * as Metrics from 'components/common/Metrics';
import PageHeading from 'components/common/PageHeading/PageHeading';
import { ActionButton } from 'components/common/ActionComponent';
import Tooltip from 'components/common/Tooltip/Tooltip';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { Action, ConnectorState, ResourceType } from 'generated-sources';
import { useConnectors, useConnects } from 'lib/hooks/api/kafkaConnect';

import List from './List';

interface CreateConnectorButtonProps {
  disabled?: boolean;
}

const CreateConnectorButton: React.FC<CreateConnectorButtonProps> = ({
  disabled = false,
}) => (
  <ActionButton
    buttonType="primary"
    buttonSize="M"
    disabled={disabled}
    to={clusterConnectorNewRelativePath}
    permission={{
      resource: ResourceType.CONNECT,
      action: Action.CREATE,
    }}
  >
    Create Connector
  </ActionButton>
);

const ListPage: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connects = [] } = useConnects(clusterName);

  // Fetches all connectors from the API, without search criteria. Used to display general metrics.
  const { data: connectorsMetrics, isLoading } = useConnectors(clusterName);

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
        {!isReadOnly &&
          (connects && connects.length > 0 ? (
            <CreateConnectorButton />
          ) : (
            <Tooltip
              value={<CreateConnectorButton disabled />}
              content="No connects available"
              placement="left"
            />
          ))}
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
        </Metrics.Section>
      </Metrics.Wrapper>
      <ControlPanelWrapper hasInput>
        <Search placeholder="Search by Connect Name, Status or Type" />
      </ControlPanelWrapper>
      <Suspense fallback={<PageLoader />}>
        <List />
      </Suspense>
    </>
  );
};

export default ListPage;
