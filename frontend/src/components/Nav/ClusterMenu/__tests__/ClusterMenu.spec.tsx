import React from 'react';
import { screen } from '@testing-library/react';
import { Cluster, ClusterFeaturesEnum } from 'generated-sources';
import ClusterMenu from 'components/Nav/ClusterMenu/ClusterMenu';
import userEvent from '@testing-library/user-event';
import { clusterConnectorsPath } from 'lib/paths';
import { render } from 'lib/testHelpers';
import { onlineClusterPayload } from 'lib/fixtures/clusters';

/*
 Due to jsdom doesnt know about scrollIntoView
*/
window.HTMLElement.prototype.scrollIntoView = jest.fn();

describe('ClusterMenu', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  const setupComponent = (cluster: Cluster, opened?: boolean) => (
    <ClusterMenu
      name={cluster.name}
      status={cluster.status}
      features={cluster.features}
      opened={opened}
    />
  );
  const getMenuItems = () => screen.getAllByRole('menuitem');
  const getBrokers = () => screen.getByTitle('Brokers');
  const getTopics = () => screen.getByTitle('Topics');
  const getConsumers = () => screen.getByTitle('Consumers');
  const getKafkaConnect = () => screen.getByTitle('Kafka Connect');
  const getCluster = () => screen.getByText(onlineClusterPayload.name);

  const clickChevron = async () => {
    const chevronSvg = document.querySelector('svg[viewBox="0 0 10 6"]');
    if (chevronSvg) {
      await userEvent.click(chevronSvg as Element);
    }
  };

  it('renders cluster menu with default set of features', async () => {
    render(setupComponent(onlineClusterPayload));
    expect(getCluster()).toBeInTheDocument();

    expect(getMenuItems().length).toEqual(1);
    await clickChevron();
    expect(getMenuItems().length).toEqual(4);

    expect(getBrokers()).toBeInTheDocument();
    expect(getTopics()).toBeInTheDocument();
    expect(getConsumers()).toBeInTheDocument();
  });

  it('renders cluster menu with correct set of features', async () => {
    render(
      setupComponent({
        ...onlineClusterPayload,
        features: [
          ClusterFeaturesEnum.SCHEMA_REGISTRY,
          ClusterFeaturesEnum.KAFKA_CONNECT,
          ClusterFeaturesEnum.KSQL_DB,
        ],
      })
    );
    expect(getMenuItems().length).toEqual(1);
    await clickChevron();
    expect(getMenuItems().length).toEqual(7);

    expect(getBrokers()).toBeInTheDocument();
    expect(getTopics()).toBeInTheDocument();
    expect(getConsumers()).toBeInTheDocument();
    expect(screen.getByTitle('Schema Registry')).toBeInTheDocument();
    expect(getKafkaConnect()).toBeInTheDocument();
    expect(screen.getByTitle('KSQL DB')).toBeInTheDocument();
  });

  it('renders open cluster menu', () => {
    render(setupComponent(onlineClusterPayload, true), {
      initialEntries: [clusterConnectorsPath(onlineClusterPayload.name)],
    });

    expect(getMenuItems().length).toEqual(4);
    expect(getCluster()).toBeInTheDocument();
    expect(getBrokers()).toBeInTheDocument();
    expect(getTopics()).toBeInTheDocument();
    expect(getConsumers()).toBeInTheDocument();
  });

  it('makes Kafka Connect link active', async () => {
    render(
      setupComponent({
        ...onlineClusterPayload,
        features: [ClusterFeaturesEnum.KAFKA_CONNECT],
      }),
      { initialEntries: [clusterConnectorsPath(onlineClusterPayload.name)] }
    );
    expect(getMenuItems().length).toEqual(1);
    await clickChevron();
    expect(getMenuItems().length).toEqual(5);

    const kafkaConnect = getKafkaConnect();
    expect(kafkaConnect).toBeInTheDocument();

    expect(getKafkaConnect()).toHaveClass('active');
  });
});
