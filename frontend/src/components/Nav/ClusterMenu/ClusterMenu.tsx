import React, { FC } from 'react';
import { Cluster, ClusterFeaturesEnum } from 'generated-sources';
import * as S from 'components/Nav/Nav.styled';
import MenuTab from 'components/Nav/Menu/MenuTab';
import MenuItem from 'components/Nav/Menu/MenuItem';
import {
  clusterACLPath,
  clusterConnectorsPath,
  clusterConsumerGroupsPath,
  clusterKsqlDbPath,
  clusterSchemasPath,
  clusterTopicsPath,
  clusterBrokersPath,
} from 'lib/paths';
import { useLocation } from 'react-router-dom';

interface ClusterMenuProps {
  name: Cluster['name'];
  status: Cluster['status'];
  features: Cluster['features'];
  openTab?: string | false;
  onTabClick: (tabName: string) => void;
}

const ClusterMenu: FC<ClusterMenuProps> = ({
  name,
  status,
  features,
  openTab,
  onTabClick,
}) => {
  const location = useLocation();

  const getIsMenuItemActive = (path: string) =>
    location.pathname.includes(path);

  const hasFeatureConfigured = (key: ClusterFeaturesEnum) =>
    features?.includes(key);

  return (
    <ul role="menu">
      <MenuTab
        title={name}
        status={status}
        isOpen={openTab === name}
        onClick={() => onTabClick(name)}
      />
      <S.AccordionContent isOpen={openTab === name}>
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
      </S.AccordionContent>
    </ul>
  );
};

export default ClusterMenu;
