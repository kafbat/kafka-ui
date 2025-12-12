import React, { ComponentProps, FC } from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import type { ClusterName } from 'lib/interfaces/cluster';
import PageHeading from 'components/common/PageHeading/PageHeading';

type ResourcePageHeadingProps = ComponentProps<typeof PageHeading>;

const ResourcePageHeading: FC<ResourcePageHeadingProps> = (props) => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();

  return <PageHeading {...props} title={clusterName} />;
};

export default ResourcePageHeading;
