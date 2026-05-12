import { ColumnDef } from '@tanstack/react-table';
import AclsTable from 'components/ACLPage/Table/Table';
import {
  createPrincipalCell,
  createHostCell,
  createOperationCell,
  createParmissionCell,
} from 'components/ACLPage/Table/TableCells';
import { KafkaAcl } from 'generated-sources';
import { useTopicAcls } from 'lib/hooks/api/topics';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import React from 'react';
import { useTheme } from 'styled-components';

const EMPTY_ACLS: KafkaAcl[] = [];
const Acls = () => {
  const { topicName, clusterName } = useAppParams<RouteParamsClusterTopic>();
  const theme = useTheme();
  const { data: acls = EMPTY_ACLS } = useTopicAcls({
    clusterName,
    topicName,
  });

  const columns = React.useMemo<ColumnDef<KafkaAcl>[]>(() => {
    return [
      createPrincipalCell(),
      createOperationCell(),
      createParmissionCell(),
      createHostCell(),
    ];
  }, [theme]);

  return <AclsTable acls={acls} columns={columns} />;
};

export default Acls;
