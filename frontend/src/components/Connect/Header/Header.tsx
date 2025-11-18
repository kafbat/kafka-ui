import { ActionButton } from 'components/common/ActionComponent';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import Tooltip from 'components/common/Tooltip/Tooltip';
import ClusterContext from 'components/contexts/ClusterContext';
import {
  Action,
  ConnectorColumnsToSort,
  ResourceType,
  SortOrder,
} from 'generated-sources';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterConnectorNewPath,
  clusterConnectorsRelativePath,
  ClusterNameRoute,
  kafkaConnectClustersRelativePath,
} from 'lib/paths';
import React from 'react';
import { DownloadCsvButton } from 'components/common/DownloadCsvButton/DownloadCsvButton';
import { kafkaConnectApiClient } from 'lib/api';
import { connects } from 'lib/fixtures/kafkaConnect';
import { useSearchParams } from 'react-router-dom';
import useFts from 'components/common/Fts/useFts';

type ConnectPage =
  | typeof kafkaConnectClustersRelativePath
  | typeof clusterConnectorsRelativePath;

const Header = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { '*': currentPath, clusterName } = useAppParams<
    ClusterNameRoute & { ['*']: ConnectPage }
  >();
  const [searchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('connects');

  const fetchCsv = async () => {
    if (currentPath === kafkaConnectClustersRelativePath) {
      return kafkaConnectApiClient.getConnectsCsv({
        clusterName,
        withStats: true,
      });
    }

    if (currentPath === clusterConnectorsRelativePath) {
      return kafkaConnectApiClient.getAllConnectorsCsv({
        clusterName,
        search: searchParams.get('q') || undefined,
        fts: isFtsEnabled,
        orderBy:
          (searchParams.get('sortBy') as ConnectorColumnsToSort) || undefined,
        sortOrder:
          (searchParams.get('sortDirection')?.toUpperCase() as SortOrder) ||
          undefined,
      });
    }

    return '';
  };

  return (
    <ResourcePageHeading text="Kafka Connect">
      {!isReadOnly && (
        <Tooltip
          value={
            <ActionButton
              name="Create Connector"
              buttonType="primary"
              buttonSize="M"
              disabled={!connects.length}
              to={`${clusterConnectorNewPath(clusterName)}`}
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

      <DownloadCsvButton
        filePrefix={`connectors-${clusterName}`}
        fetchCsv={fetchCsv}
      />
    </ResourcePageHeading>
  );
};

export default Header;
