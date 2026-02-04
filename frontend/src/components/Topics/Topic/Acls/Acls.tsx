import { ColumnDef } from '@tanstack/react-table';
import AclsTable from 'components/ACLPage/Table/Table';
import {
  createPrincipalCell,
  createResourceCell,
  createHostCell,
  createOperationCell,
  createParmissionCell,
  createNameCell,
} from 'components/ACLPage/Table/TableCells';
import { KafkaAcl } from 'generated-sources';
import { aclPayload } from 'lib/fixtures/acls';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import React from 'react';
import { useTheme } from 'styled-components';

const Acls = () => {
  const { topicName } = useAppParams<RouteParamsClusterTopic>();
  const theme = useTheme();
  const acls = aclPayload; // TODO: here will be a query

  const columns = React.useMemo<ColumnDef<KafkaAcl>[]>(() => {
    return [
      createPrincipalCell(),
      createResourceCell(),
      createNameCell({ topicName }),
      createOperationCell(),
      createParmissionCell(),
      createHostCell(),
    ];
  }, [theme]);

  return <AclsTable acls={acls} columns={columns} />;
};

export default Acls;
