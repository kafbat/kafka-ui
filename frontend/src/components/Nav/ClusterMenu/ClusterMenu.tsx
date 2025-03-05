import React, { type FC, useState } from 'react';
import { Cluster, ClusterFeaturesEnum } from 'generated-sources';
import * as S from 'components/Nav/Nav.styled';
import MenuTab from 'components/Nav/Menu/MenuTab';
import MenuItem from 'components/Nav/Menu/MenuItem';
import {
  clusterACLPath,
  clusterAclRelativePath,
  clusterBrokerRelativePath,
  clusterBrokersPath,
  clusterConnectorsPath,
  clusterConnectorsRelativePath,
  clusterConsumerGroupsPath,
  clusterConsumerGroupsRelativePath,
  clusterKsqlDbPath,
  clusterKsqlDbRelativePath,
  clusterSchemasPath,
  clusterSchemasRelativePath,
  clusterTopicsPath,
  clusterTopicsRelativePath,
} from 'lib/paths';
import { useLocation } from 'react-router-dom';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import { ClusterColorKey } from 'theme/theme';

interface ClusterMenuProps {
  name: Cluster['name'];
  status: Cluster['status'];
  features: Cluster['features'];
  singleMode?: boolean;
}

const ClusterMenu: FC<ClusterMenuProps> = ({
  name,
  status,
  features,
  singleMode,
}) => {
  const hasFeatureConfigured = (key: ClusterFeaturesEnum) =>
    features?.includes(key);
  const [isOpen, setIsOpen] = useState(!!singleMode);
  const location = useLocation();
  const [colorKey, setColorKey] = useLocalStorage<ClusterColorKey>(
    `clusterColor-${name}`,
    'transparent'
  );

  const getIsMenuItemActive = (path: string) =>
    location.pathname.includes(path);

  return (
    <S.ClusterList role="menu" $colorKey={colorKey}>
      <MenuTab
        title={name}
        status={status}
        isOpen={isOpen}
        toggleClusterMenu={() => setIsOpen((prev) => !prev)}
        setColorKey={setColorKey}
      />
      {isOpen && (
        <S.List>
          <MenuItem
            isActive={getIsMenuItemActive(clusterBrokerRelativePath)}
            to={clusterBrokersPath(name)}
            title="Brokers"
          />
          <MenuItem
            isActive={getIsMenuItemActive(clusterTopicsRelativePath)}
            to={clusterTopicsPath(name)}
            title="Topics"
          />
          <MenuItem
            isActive={getIsMenuItemActive(clusterConsumerGroupsRelativePath)}
            to={clusterConsumerGroupsPath(name)}
            title="Consumers"
          />
          {hasFeatureConfigured(ClusterFeaturesEnum.SCHEMA_REGISTRY) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterSchemasRelativePath)}
              to={clusterSchemasPath(name)}
              title="Schema Registry"
            />
          )}
          {hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_CONNECT) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterConnectorsRelativePath)}
              to={clusterConnectorsPath(name)}
              title="Kafka Connect"
            />
          )}
          {hasFeatureConfigured(ClusterFeaturesEnum.KSQL_DB) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterKsqlDbRelativePath)}
              to={clusterKsqlDbPath(name)}
              title="KSQL DB"
            />
          )}
          {(hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_ACL_VIEW) ||
            hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_ACL_EDIT)) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterAclRelativePath)}
              to={clusterACLPath(name)}
              title="ACL"
            />
          )}
        </S.List>
      )}
    </S.ClusterList>
  );
};

export default ClusterMenu;
