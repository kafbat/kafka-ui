import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterConsumerGroupResetRelativePath,
  clusterConsumerGroupsPath,
  ClusterGroupParam,
  clusterConnectorsPath,
} from 'lib/paths';
import Search from 'components/common/Search/Search';
import ClusterContext from 'components/contexts/ClusterContext';
import * as Metrics from 'components/common/Metrics';
import { Tag } from 'components/common/Tag/Tag.styled';
import getTagColor from 'components/common/Tag/getTagColor';
import { Dropdown } from 'components/common/Dropdown';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import { Action, ConsumerGroupState, ResourceType } from 'generated-sources';
import { ActionDropdownItem } from 'components/common/ActionComponent';
import {
  useConsumerGroupDetails,
  useDeleteConsumerGroupMutation,
} from 'lib/hooks/api/consumers';
import Tooltip from 'components/common/Tooltip/Tooltip';
import { CONSUMER_GROUP_STATE_TOOLTIPS } from 'lib/constants';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import { exportTableCSV, TableProvider } from 'components/common/NewTable';
import { Button } from 'components/common/Button/Button';
import ExportIcon from 'components/common/Icons/ExportIcon';
import PageLoader from 'components/common/PageLoader/PageLoader';
import ErrorPage from 'components/ErrorPage/ErrorPage';
import { getConnectorNameFromConsumerGroup } from 'lib/utils/connectorUtils';

import { TopicsTable } from './TopicsTable/TopicsTable';

const Details: React.FC = () => {
  const navigate = useNavigate();
  const { isReadOnly } = React.useContext(ClusterContext);
  const routeParams = useAppParams<ClusterGroupParam>();
  const { clusterName, consumerGroupID } = routeParams;

  const {
    data: consumerGroup,
    error,
    isSuccess,
    refetch,
    isLoading,
    isRefetching,
  } = useConsumerGroupDetails(routeParams);
  const deleteConsumerGroup = useDeleteConsumerGroupMutation(routeParams);

  const onDelete = async () => {
    await deleteConsumerGroup.mutateAsync();
    navigate('../');
  };

  const onResetOffsets = () => {
    navigate(clusterConsumerGroupResetRelativePath);
  };

  const hasAssignedTopics = consumerGroup?.topics !== 0;
  const connectorName = getConnectorNameFromConsumerGroup(consumerGroupID);

  return (
    <TableProvider>
      {({ table }) => {
        const handleExportClick = () => {
          exportTableCSV(table, { prefix: 'connector-topics' });
        };

        return (
          <>
            <div>
              <ResourcePageHeading
                text={consumerGroupID}
                backTo={clusterConsumerGroupsPath(clusterName)}
                backText="Consumers"
              >
                <Button
                  buttonType="secondary"
                  buttonSize="M"
                  onClick={handleExportClick}
                >
                  <ExportIcon /> Export CSV
                </Button>

                {!isReadOnly && (
                  <Dropdown>
                    <ActionDropdownItem
                      onClick={onResetOffsets}
                      permission={{
                        resource: ResourceType.CONSUMER,
                        action: Action.RESET_OFFSETS,
                        value: consumerGroupID,
                      }}
                      disabled={!hasAssignedTopics}
                    >
                      Reset offset
                    </ActionDropdownItem>
                    <ActionDropdownItem
                      confirm="Are you sure you want to delete this consumer group?"
                      onClick={onDelete}
                      danger
                      permission={{
                        resource: ResourceType.CONSUMER,
                        action: Action.DELETE,
                        value: consumerGroupID,
                      }}
                    >
                      Delete consumer group
                    </ActionDropdownItem>
                  </Dropdown>
                )}
              </ResourcePageHeading>
            </div>

            {(isLoading || isRefetching) && <PageLoader />}

            {error && (
              <ErrorPage
                status={error.status}
                onClick={refetch}
                resourceName={`Consumer Group ${consumerGroupID}`}
              />
            )}

            {isSuccess && (
              <>
                {' '}
                <Metrics.Wrapper>
                  <Metrics.Section>
                    <Metrics.Indicator label="State">
                      <Tooltip
                        value={
                          <Tag color={getTagColor(consumerGroup?.state)}>
                            {consumerGroup?.state}
                          </Tag>
                        }
                        content={
                          CONSUMER_GROUP_STATE_TOOLTIPS[
                            consumerGroup?.state || ConsumerGroupState.UNKNOWN
                          ]
                        }
                        placement="bottom-start"
                      />
                    </Metrics.Indicator>
                    <Metrics.Indicator label="Members">
                      {consumerGroup?.members}
                    </Metrics.Indicator>
                    <Metrics.Indicator label="Assigned Topics">
                      {consumerGroup?.topics}
                    </Metrics.Indicator>
                    <Metrics.Indicator label="Assigned Partitions">
                      {consumerGroup?.partitions?.length}
                    </Metrics.Indicator>
                    <Metrics.Indicator label="Coordinator ID">
                      {consumerGroup?.coordinator?.id}
                    </Metrics.Indicator>
                    <Metrics.Indicator label="Total lag">
                      {consumerGroup?.consumerLag}
                    </Metrics.Indicator>
                    {connectorName && (
                      <Metrics.Indicator label="Connector">
                        <Link
                          to={`${clusterConnectorsPath(clusterName)}?search=${encodeURIComponent(connectorName)}`}
                        >
                          {connectorName}
                        </Link>
                      </Metrics.Indicator>
                    )}
                  </Metrics.Section>
                </Metrics.Wrapper>
                <ControlPanelWrapper hasInput style={{ margin: '16px 0 20px' }}>
                  <Search placeholder="Search by Topic Name" />
                </ControlPanelWrapper>
                <TopicsTable partitions={consumerGroup?.partitions ?? []} />
              </>
            )}
          </>
        );
      }}
    </TableProvider>
  );
};

export default Details;
