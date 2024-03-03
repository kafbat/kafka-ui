import React, { useState } from 'react';
import { ColumnDef } from '@tanstack/react-table';
import PageHeading from 'components/common/PageHeading/PageHeading';
import Table from 'components/common/NewTable';
import { useConfirm } from 'lib/hooks/useConfirm';
import useAppParams from 'lib/hooks/useAppParams';
import { useAcls, useDeleteAcl } from 'lib/hooks/api/acl';
import { ClusterName } from 'redux/interfaces';
import {
  KafkaAcl,
  KafkaAclNamePatternType,
  KafkaAclPermissionEnum,
} from 'generated-sources';
import useBoolean from 'lib/hooks/useBoolean';
import { Button } from 'components/common/Button/Button';
import ACLForm from 'components/ACLPage/Form/Form';

import * as S from './List.styled';
import ActionsCell from './ActionsCell';

const ACList: React.FC = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const { data: aclList } = useAcls(clusterName);
  const { deleteResource } = useDeleteAcl(clusterName);
  const modal = useConfirm(true);
  const {
    value: isFormOpen,
    setFalse: closeForm,
    setTrue: openFrom,
  } = useBoolean();

  const handleDeleteClick = (acl: KafkaAcl) => {
    modal('Are you sure want to delete this ACL record?', () =>
      deleteResource(acl)
    );
  };
  const [currentAcl, setAcl] = useState<KafkaAcl | null>(null);

  const handleEditClick = (acl: KafkaAcl) => {
    setAcl(acl);
    openFrom();
  };
  const handleCreateClick = () => {
    setAcl(null);
    openFrom();
  };

  const columns = React.useMemo<ColumnDef<KafkaAcl>[]>(
    () => [
      {
        header: 'Principal',
        accessorKey: 'principal',
        size: 257,
      },
      {
        header: 'Resource',
        accessorKey: 'resourceType',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <S.EnumCell>{getValue<string>().toLowerCase()}</S.EnumCell>
        ),
        size: 145,
      },
      {
        header: 'Pattern',
        accessorKey: 'resourceName',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue, row }) => {
          let chipType;
          if (
            row.original.namePatternType === KafkaAclNamePatternType.PREFIXED
          ) {
            chipType = 'default';
          }

          if (
            row.original.namePatternType === KafkaAclNamePatternType.LITERAL
          ) {
            chipType = 'secondary';
          }
          return (
            <S.PatternCell>
              {getValue<string>()}
              {chipType ? (
                <S.Chip chipType={chipType}>
                  {row.original.namePatternType.toLowerCase()}
                </S.Chip>
              ) : null}
            </S.PatternCell>
          );
        },
        size: 257,
      },
      {
        header: 'Host',
        accessorKey: 'host',
        size: 257,
      },
      {
        header: 'Operation',
        accessorKey: 'operation',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <S.EnumCell>{getValue<string>().toLowerCase()}</S.EnumCell>
        ),
        size: 121,
      },
      {
        header: 'Permission',
        accessorKey: 'permission',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <S.Chip
            chipType={
              getValue<string>() === KafkaAclPermissionEnum.ALLOW
                ? 'success'
                : 'danger'
            }
          >
            {getValue<string>().toLowerCase()}
          </S.Chip>
        ),
        size: 111,
      },
      {
        id: 'actions',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ row }) => {
          return (
            <ActionsCell
              onDelete={() => handleDeleteClick(row.original)}
              onEdit={() => handleEditClick(row.original)}
            />
          );
        },
      },
    ],
    []
  );

  return (
    <S.Container>
      <PageHeading text="Access Control List">
        <Button buttonType="primary" buttonSize="M" onClick={handleCreateClick}>
          + Create ACL
        </Button>
      </PageHeading>
      <Table
        columns={columns}
        data={aclList ?? []}
        emptyMessage="No ACL items found"
      />
      <ACLForm open={isFormOpen} onClose={closeForm} acl={currentAcl} />
    </S.Container>
  );
};

export default ACList;
