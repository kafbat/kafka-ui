import React, { type FC, useState } from 'react';
import { Cluster, ClusterFeaturesEnum } from 'generated-sources';
import * as S from 'components/Nav/Nav.styled';
import MenuTab from 'components/Nav/Menu/MenuTab';
import MenuItem from 'components/Nav/Menu/MenuItem';
import {
  clusterACLPath,
  clusterBrokersPath,
  clusterConnectorsPath,
  clusterConsumerGroupsPath,
  clusterKsqlDbPath,
  clusterSchemasPath,
  clusterTopicsPath,
} from 'lib/paths';
import { useLocation } from 'react-router-dom';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import { ClusterColorKey } from 'theme/theme';

interface ClusterMenuProps {
  name: Cluster['name'];
  status: Cluster['status'];
  features: Cluster['features'];
  opened?: boolean;
}

const ClusterMenu: FC<ClusterMenuProps> = ({
  name,
  status,
  features,
  opened,
}) => {
  const hasFeatureConfigured = (key: ClusterFeaturesEnum) =>
    features?.includes(key);
  const [isOpen, setIsOpen] = useState(!!opened);
  const location = useLocation();
  const [colorKey, setColorKey] = useLocalStorage<ClusterColorKey>(
    `clusterColor-${name}`,
    'transparent'
  );

  const getIsMenuItemActive = (path: string) => {
    return location.pathname.includes(path);
  };

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
            isActive={getIsMenuItemActive(clusterBrokersPath(name))}
            to={clusterBrokersPath(name)}
            title="Brokers"
          />
          <MenuItem
            isActive={getIsMenuItemActive(clusterTopicsPath(name))}
            to={clusterTopicsPath(name)}
            title="Topics"
          />
          <MenuItem
            isActive={getIsMenuItemActive(clusterConsumerGroupsPath(name))}
            to={clusterConsumerGroupsPath(name)}
            title="Consumers"
          />
          {hasFeatureConfigured(ClusterFeaturesEnum.SCHEMA_REGISTRY) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterSchemasPath(name))}
              to={clusterSchemasPath(name)}
              title="Schema Registry"
            />
          )}
          {hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_CONNECT) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterConnectorsPath(name))}
              to={clusterConnectorsPath(name)}
              title="Kafka Connect"
            />
          )}
          {hasFeatureConfigured(ClusterFeaturesEnum.KSQL_DB) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterKsqlDbPath(name))}
              to={clusterKsqlDbPath(name)}
              title="KSQL DB"
            />
          )}
          {(hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_ACL_VIEW) ||
            hasFeatureConfigured(ClusterFeaturesEnum.KAFKA_ACL_EDIT)) && (
            <MenuItem
              isActive={getIsMenuItemActive(clusterACLPath(name))}
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
