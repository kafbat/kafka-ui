import { ActionButton } from 'components/common/ActionComponent';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import Tooltip from 'components/common/Tooltip/Tooltip';
import ClusterContext from 'components/contexts/ClusterContext';
import { ResourceType, Action } from 'generated-sources';
import { useConnects } from 'lib/hooks/api/kafkaConnect';
import useAppParams from 'lib/hooks/useAppParams';
import {
  clusterConnectorNewPath,
  clusterConnectorsRelativePath,
  ClusterNameRoute,
  kafkaConnectClustersRelativePath,
} from 'lib/paths';
import React from 'react';
import { exportTableCSV, useTableInstance } from 'components/common/NewTable';
import { Button } from 'components/common/Button/Button';
import ExportIcon from 'components/common/Icons/ExportIcon';

type ConnectPage =
  | typeof kafkaConnectClustersRelativePath
  | typeof clusterConnectorsRelativePath;

const getCsvPrefix = (page: ConnectPage) => {
  let prefix = 'kafka-connect';

  if (page === clusterConnectorsRelativePath) {
    prefix += '-connectors';
  }

  if (page === kafkaConnectClustersRelativePath) {
    prefix += '-clusters';
  }

  return prefix;
};

const Header = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { '*': currentPath, clusterName } = useAppParams<
    ClusterNameRoute & { ['*']: ConnectPage }
  >();
  const { data: connects = [] } = useConnects(clusterName, true);

  const instance = useTableInstance();

  const handleExportClick = () => {
    exportTableCSV(instance?.table, { prefix: getCsvPrefix(currentPath) });
  };

  return (
    <ResourcePageHeading text="Kafka Connect">
      {!isReadOnly && (
        <Tooltip
          value={
            <ActionButton
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

      <Button buttonType="secondary" buttonSize="M" onClick={handleExportClick}>
        <ExportIcon /> Export CSV
      </Button>
    </ResourcePageHeading>
  );
};

export default Header;
