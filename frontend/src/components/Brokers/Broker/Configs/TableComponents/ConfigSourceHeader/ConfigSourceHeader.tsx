import React from 'react';
import Tooltip from 'components/common/Tooltip/Tooltip';
import InfoIcon from 'components/common/Icons/InfoIcon';
import { CONFIG_SOURCE_NAME_MAP } from 'components/Brokers/Broker/Configs/lib/constants';

import * as S from './ConfigSourceHeader.styled';

const tooltipContent = `${CONFIG_SOURCE_NAME_MAP.DYNAMIC_TOPIC_CONFIG} = dynamic topic config that is configured for a specific topic
${CONFIG_SOURCE_NAME_MAP.DYNAMIC_BROKER_LOGGER_CONFIG} = dynamic broker logger config that is configured for a specific broker
${CONFIG_SOURCE_NAME_MAP.DYNAMIC_BROKER_CONFIG} = dynamic broker config that is configured for a specific broker
${CONFIG_SOURCE_NAME_MAP.DYNAMIC_DEFAULT_BROKER_CONFIG} = dynamic broker config that is configured as default for all brokers in the cluster
${CONFIG_SOURCE_NAME_MAP.STATIC_BROKER_CONFIG} = static broker config provided as broker properties at start up (e.g. server.properties file)
${CONFIG_SOURCE_NAME_MAP.DEFAULT_CONFIG} = built-in default configuration for configs that have a default value
${CONFIG_SOURCE_NAME_MAP.UNKNOWN} = source unknown e.g. in the ConfigEntry used for alter requests where source is not set`;

const ConfigSourceHeader = () => (
  <S.Source>
    Source
    <Tooltip
      value={<InfoIcon />}
      content={tooltipContent}
      placement="top-end"
    />
  </S.Source>
);

export default ConfigSourceHeader;
