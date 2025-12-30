import React, { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ColumnDef, Row } from '@tanstack/react-table';
import Table from 'components/common/NewTable';
import { useConfirm } from 'lib/hooks/useConfirm';
import useAppParams from 'lib/hooks/useAppParams';
import { useAcls, useDeleteAcl } from 'lib/hooks/api/acl';
import { ClusterName } from 'lib/interfaces/cluster';
import {
  Action,
  KafkaAcl,
  KafkaAclNamePatternType,
  KafkaAclPermissionEnum,
  ResourceType,
} from 'generated-sources';
import useBoolean from 'lib/hooks/useBoolean';
import ACLForm from 'components/ACLPage/Form/Form';
import DeleteIcon from 'components/common/Icons/DeleteIcon';
import { useTheme } from 'styled-components';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import PlusIcon from 'components/common/Icons/PlusIcon';
import ActionButton from 'components/common/ActionComponent/ActionButton/ActionButton';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Search from 'components/common/Search/Search';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';
import { ActionPermissionWrapper } from 'components/common/ActionComponent';
import useFts from 'components/common/Fts/useFts';
import Fts from 'components/common/Fts/Fts';
import ClusterContext from 'components/contexts/ClusterContext';

import * as S from './List.styled';

const ACList: React.FC = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('acl');
  const { data: aclList } = useAcls({
    clusterName,
    search: searchParams.get('q') ?? '',
    fts: isFtsEnabled,
  });
  const { deleteResource } = useDeleteAcl(clusterName);
  const { isReadOnly } = React.useContext(ClusterContext);
  const modal = useConfirm(true);
  const theme = useTheme();
  const {
    value: isFormOpen,
    setFalse: closeForm,
    setTrue: openFrom,
  } = useBoolean();
  const [rowId, setRowId] = React.useState('');

  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (searchParams.get('q')) {
      params.set('page', '1'); // reset to first page on new search
    } else {
      params.delete('q');
    }
    setSearchParams(params, { replace: true });
  }, [searchParams.get('q')]);

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
        cell: BreakableTextCell,
      },
      {
        header: 'Resource',
        accessorKey: 'resourceType',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <S.EnumCell>{getValue<string>().toLowerCase()}</S.EnumCell>
        ),
        filterFn: 'arrIncludesSome',
        meta: {
          filterVariant: 'multi-select',
        },
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
        filterFn: 'includesString',
        meta: {
          filterVariant: 'text',
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
        filterFn: 'arrIncludesSome',
        meta: {
          filterVariant: 'multi-select',
        },
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
            <ActionPermissionWrapper
              onAction={() => handleDeleteClick(row.original)}
              permission={{
                resource: ResourceType.ACL,
                action: Action.EDIT,
              }}
            >
              <S.DeleteCell>
                <DeleteIcon
                  fill={
                    rowId === row.id
                      ? theme.acl.table.deleteIcon
                      : 'transparent'
                  }
                />
              </S.DeleteCell>
            </ActionPermissionWrapper>
          );
        },
        size: 76,
      },
    ],
    [rowId]
  );

  const filterPersister = useQueryPersister(columns);

  return (
    <S.Container>
      <ResourcePageHeading text="Access Control List">
        <ActionButton
          buttonType="primary"
          buttonSize="M"
          onClick={openFrom}
          disabled={isReadOnly}
          permission={{
            resource: ResourceType.ACL,
            action: Action.EDIT,
          }}
        >
          <PlusIcon /> Create ACL
        </ActionButton>
      </ResourcePageHeading>
      <ControlPanelWrapper hasInput>
        <Search
          key={clusterName}
          placeholder="Search by Principal Name"
          extraActions={<Fts resourceName="acl" />}
        />
      </ControlPanelWrapper>
      <Table
        columns={columns}
        data={aclList ?? []}
        emptyMessage="No ACL items found"
        onRowHover={handleRowHover}
        onMouseLeave={() => setRowId('')}
        filterPersister={filterPersister}
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
