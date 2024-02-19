import React from 'react';
import { clusterConnectorNewRelativePath } from 'lib/paths';
import { ActionButton } from 'components/common/ActionComponent';
import { Action, ResourceType } from 'generated-sources';

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

export default CreateConnectorButton;
