import { ColumnDef } from '@tanstack/react-table';
import Table from 'components/common/NewTable';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';
import { KafkaAcl } from 'generated-sources';
import React, { FC } from 'react';

type AclsTableProps = { acls: KafkaAcl[]; columns: ColumnDef<KafkaAcl>[] };

const AclsTable: FC<AclsTableProps> = ({ acls, columns }) => {
  const filterPersister = useQueryPersister(columns);
  return (
    <Table
      columns={columns}
      data={acls}
      emptyMessage="No ACL items found"
      filterPersister={filterPersister}
      enableSorting
    />
  );
};

export default AclsTable;
