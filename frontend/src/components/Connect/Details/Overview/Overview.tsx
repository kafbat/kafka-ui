import React, { useState } from 'react';
import * as C from 'components/common/Tag/Tag.styled';
import * as Metrics from 'components/common/Metrics';
import { Button } from 'components/common/Button/Button';
import getTagColor from 'components/common/Tag/getTagColor';
import { RouterParamsClusterConnectConnector } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import { useConnector, useConnectorTasks } from 'lib/hooks/api/kafkaConnect';
import { ConnectorState } from 'generated-sources';

import getTaskMetrics from './getTaskMetrics';
import * as S from './Overview.styled';

const Overview: React.FC = () => {
  const routerProps = useAppParams<RouterParamsClusterConnectConnector>();
  const [showTraceModal, setShowTraceModal] = useState(false);

  const { data: connector } = useConnector(routerProps);
  const { data: tasks } = useConnectorTasks(routerProps);

  if (!connector) {
    return null;
  }

  const { running, failed } = getTaskMetrics(tasks);

  const hasTraceInfo = connector.status.trace;

  const handleStateClick = () => {
    if (connector.status.state === ConnectorState.FAILED && hasTraceInfo) {
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
              style={{
                cursor:
                  connector.status.state === ConnectorState.FAILED &&
                  hasTraceInfo
                    ? 'pointer'
                    : 'default',
              }}
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
        </Metrics.Section>
      </Metrics.Wrapper>

      {showTraceModal && (
        <S.ModalOverlay onClick={() => setShowTraceModal(false)}>
          <S.ModalContent
            onClick={(e: React.MouseEvent) => e.stopPropagation()}
          >
            <S.ModalHeader>
              <div>
                <S.ModalTitle>Connector Error Details</S.ModalTitle>
                {connector.status.workerId && (
                  <S.WorkerInfo>
                    Worker: {connector.status.workerId}
                  </S.WorkerInfo>
                )}
              </div>
            </S.ModalHeader>

            <S.TraceContent>
              {connector.status.trace ? (
                <div>{connector.status.trace}</div>
              ) : null}
            </S.TraceContent>

            <S.ModalFooter>
              <Button
                buttonType="primary"
                buttonSize="M"
                onClick={() => setShowTraceModal(false)}
              >
                Close
              </Button>
            </S.ModalFooter>
          </S.ModalContent>
        </S.ModalOverlay>
      )}
    </>
  );
};

export default Overview;
