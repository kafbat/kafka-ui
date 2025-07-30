import { ActionButton } from 'components/common/ActionComponent';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import Tooltip from 'components/common/Tooltip/Tooltip';
import ClusterContext from 'components/contexts/ClusterContext';
import { ResourceType, Action } from 'generated-sources';
import { useConnects } from 'lib/hooks/api/kafkaConnect';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectorNewPath, ClusterNameRoute } from 'lib/paths';
import React from 'react';

const Header = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connects = [] } = useConnects(clusterName, true);
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
    </ResourcePageHeading>
  );
};

export default Header;
