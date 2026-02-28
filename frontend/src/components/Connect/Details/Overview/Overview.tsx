import React, { useState } from 'react';
import * as C from 'components/common/Tag/Tag.styled';
import * as Metrics from 'components/common/Metrics';
import { Button } from 'components/common/Button/Button';
import { Modal } from 'components/common/Modal';
import getTagColor from 'components/common/Tag/getTagColor';
import {
  clusterConsumerGroupDetailsPath,
  RouterParamsClusterConnectConnector,
} from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { useConnector, useConnectorTasks } from 'lib/hooks/api/kafkaConnect';
import { ConnectorState, Connector } from 'generated-sources';
import { Link, useParams } from 'react-router-dom';

import getTaskMetrics from './getTaskMetrics';
import * as S from './Overview.styled';

const Overview: React.FC = () => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const [showTraceModal, setShowTraceModal] = useState(false);
  const { clusterName } = useParams<{ clusterName: string }>();

  const { data: connector } = useConnector(routerProps);
  const { data: tasks } = useConnectorTasks(routerProps);

  if (!connector) {
    return null;
  }

  const { running, failed } = getTaskMetrics(tasks);

  const canShowTrace = (connectorData: Connector) =>
    connectorData.status.state === ConnectorState.FAILED &&
    !!connectorData.status.trace;

  const handleStateClick = () => {
    if (canShowTrace(connector)) {
      setShowTraceModal(true);
    }
  };

  return (
    <>
      <Metrics.Wrapper>
        <Metrics.Section>
          {connector.status?.workerId && (
            <Metrics.Indicator label="Worker">
              {connector.status.workerId}
            </Metrics.Indicator>
          )}
          <Metrics.Indicator label="Type">{connector.type}</Metrics.Indicator>
          {connector.config['connector.class'] && (
            <Metrics.Indicator label="Class">
              {connector.config['connector.class']}
            </Metrics.Indicator>
          )}
          <Metrics.Indicator label="State">
            <C.Tag
              color={getTagColor(connector.status.state)}
              clickable={canShowTrace(connector)}
              onClick={handleStateClick}
            >
              {connector.status.state}
            </C.Tag>
          </Metrics.Indicator>
          <Metrics.Indicator label="Tasks Running">{running}</Metrics.Indicator>
          <Metrics.Indicator
            label="Tasks Failed"
            isAlert
            alertType={failed > 0 ? 'error' : 'success'}
          >
            {failed}
          </Metrics.Indicator>
          {connector.consumer && clusterName && (
            <Metrics.Indicator label="Consumer Group">
              <Link
                to={clusterConsumerGroupDetailsPath(
                  clusterName,
                  encodeURIComponent(connector.consumer)
                )}
              >
                {connector.consumer}
              </Link>
            </Metrics.Indicator>
          )}
        </Metrics.Section>
      </Metrics.Wrapper>

      {showTraceModal && (
        <Modal
          isOpen={showTraceModal}
          onClose={() => setShowTraceModal(false)}
          title="Connector Error Details"
          footer={
            <Button
              buttonType="primary"
              buttonSize="M"
              onClick={() => setShowTraceModal(false)}
            >
              Close
            </Button>
          }
        >
          {connector.status.workerId && (
            <S.WorkerInfo>Worker: {connector.status.workerId}</S.WorkerInfo>
          )}

          {connector.status.trace && (
            <S.TraceContent>
              <div>{connector.status.trace}</div>
            </S.TraceContent>
          )}
        </Modal>
      )}
    </>
  );
};

export default Overview;
