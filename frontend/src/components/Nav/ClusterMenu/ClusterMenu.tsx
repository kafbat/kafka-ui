import React, { type FC } from 'react';
import { Cluster, ClusterFeaturesEnum } from 'generated-sources';
import * as S from 'components/Nav/Nav.styled';
import MenuTab from 'components/Nav/Menu/MenuTab';
import MenuItem from 'components/Nav/Menu/MenuItem';
import {
  clusterACLPath,
  clusterBrokersPath,
  clusterConnectsPath,
  clusterConnectorsPath,
  clusterConsumerGroupsPath,
  clusterKsqlDbPath,
  clusterSchemasPath,
  clusterTopicsPath,
  kafkaConnectPath,
} from 'lib/paths';
import { useLocation, useNavigate } from 'react-router-dom';
import { useLocalStorage } from 'lib/hooks/useLocalStorage';
import { ClusterColorKey } from 'theme/theme';
import useScrollIntoView from 'lib/hooks/useScrollIntoView';

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
  opened = false,
}) => {
  const hasFeatureConfigured = (key: ClusterFeaturesEnum) =>
    features?.includes(key);

  const [isOpen, setIsOpen] = useLocalStorage<boolean>(
    `clusterMenu-${name}-isOpen`,
    opened
  );
  const location = useLocation();
  const navigate = useNavigate();
  const [colorKey, setColorKey] = useLocalStorage<ClusterColorKey>(
    `clusterColor-${name}`,
    'transparent'
  );
  const getIsMenuItemActive = (path: string) => {
    return location.pathname.includes(path);
  };

  const { ref } = useScrollIntoView<HTMLUListElement>(opened);

  const handleClusterNameClick = () => {
    if (!isOpen) {
      setIsOpen(true);
    }
    navigate(clusterBrokersPath(name));
  };

  const handleToggleMenu = () => {
    setIsOpen((prev) => !prev);
  };

  return (
    <S.ClusterList role="menu" $colorKey={colorKey} ref={ref}>
      <MenuTab
        title={name}
        status={status}
        isOpen={isOpen}
        toggleClusterMenu={handleToggleMenu}
        onClusterNameClick={handleClusterNameClick}
        setColorKey={setColorKey}
        isActive={opened}
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
              isActive={
                getIsMenuItemActive(kafkaConnectPath(name)) ||
                getIsMenuItemActive(clusterConnectorsPath(name)) ||
                getIsMenuItemActive(clusterConnectsPath(name))
              }
              to={kafkaConnectPath(name)}
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
