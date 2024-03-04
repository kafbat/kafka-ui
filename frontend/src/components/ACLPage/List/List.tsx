import React from 'react';
import { ColumnDef, Row } from '@tanstack/react-table';
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
import DeleteIcon from 'components/common/Icons/DeleteIcon';
import { useTheme } from 'styled-components';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import PlusIcon from 'components/common/Icons/PlusIcon';

import * as S from './List.styled';

const ACList: React.FC = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const { data: aclList } = useAcls(clusterName);
  const { deleteResource } = useDeleteAcl(clusterName);
  const modal = useConfirm(true);
  const theme = useTheme();
  const {
    value: isFormOpen,
    setFalse: closeForm,
    setTrue: openFrom,
  } = useBoolean();
  const [rowId, setRowId] = React.useState('');

  const handleDeleteClick = (acl: KafkaAcl | null) => {
    if (acl) {
      modal('Are you sure want to delete this ACL record?', () =>
        deleteResource(acl)
      );
    }
  };

  const handleRowHover = (value: Row<KafkaAcl>) => {
    if (value) {
      setRowId(value.id);
    }
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
        id: 'delete',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ row }) => {
          return (
            <S.DeleteCell onClick={() => handleDeleteClick(row.original)}>
              <DeleteIcon
                fill={
                  rowId === row.id ? theme.acl.table.deleteIcon : 'transparent'
                }
              />
            </S.DeleteCell>
          );
        },
        size: 76,
      },
    ],
    [rowId]
  );

  return (
    <S.Container>
      <PageHeading text="Access Control List">
        <Button buttonType="primary" buttonSize="M" onClick={openFrom}>
          <PlusIcon /> Create ACL
        </Button>
      </PageHeading>
      <Table
        columns={columns}
        data={aclList ?? []}
        emptyMessage="No ACL items found"
        onRowHover={handleRowHover}
        onMouseLeave={() => setRowId('')}
        enableSorting
      />
      <ACLFormContext.Provider
        value={{
          close: closeForm,
        }}
      >
        {isFormOpen && <ACLForm isOpen={isFormOpen} />}
      </ACLFormContext.Provider>
    </S.Container>
  );
};

export default ACList;
