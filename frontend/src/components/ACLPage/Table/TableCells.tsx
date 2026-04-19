import React from 'react';
import { ColumnDef } from '@tanstack/react-table';
import {
  Action,
  KafkaAcl,
  KafkaAclNamePatternType,
  KafkaAclPermissionEnum,
  ResourceType,
} from 'generated-sources';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { ActionPermissionWrapper } from 'components/common/ActionComponent';
import DeleteIcon from 'components/common/Icons/DeleteIcon';
import { DefaultTheme } from 'styled-components';

import * as S from './Table.styled';

const createPrincipalCell = (): ColumnDef<KafkaAcl> => {
  return {
    header: 'Principal',
    accessorKey: 'principal',
    size: 257,
    cell: BreakableTextCell,
  };
};

const createResourceCell = (): ColumnDef<KafkaAcl> => {
  return {
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
  };
};

const createPatternCell = (): ColumnDef<KafkaAcl> => {
  return {
    header: 'Pattern',
    accessorKey: 'resourceName',
    // eslint-disable-next-line react/no-unstable-nested-components
    cell: ({ getValue, row }) => {
      let chipType;
      if (row.original.namePatternType === KafkaAclNamePatternType.PREFIXED) {
        chipType = 'default';
      }

      if (row.original.namePatternType === KafkaAclNamePatternType.LITERAL) {
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
  };
};
const createHostCell = (): ColumnDef<KafkaAcl> => {
  return {
    header: 'Host',
    accessorKey: 'host',
    size: 257,
    filterFn: 'includesString',
    meta: {
      filterVariant: 'text',
    },
  };
};
const createOperationCell = (): ColumnDef<KafkaAcl> => {
  return {
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
  };
};
const createParmissionCell = (): ColumnDef<KafkaAcl> => {
  return {
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
  };
};

const createNameCell = ({
  topicName,
}: {
  topicName: string;
}): ColumnDef<KafkaAcl> => {
  return {
    header: 'Name',
    accessorKey: 'name',
    cell: () => {
      return <div>{topicName}</div>;
    },
  };
};

const createActionsCell = ({
  onDelete,
  theme,
}: {
  onDelete: (acl: KafkaAcl | null) => void;
  theme: DefaultTheme;
}): ColumnDef<KafkaAcl> => {
  return {
    id: 'delete',
    // eslint-disable-next-line react/no-unstable-nested-components
    cell: ({ row }) => {
      return (
        <ActionPermissionWrapper
          onAction={() => onDelete(row.original)}
          permission={{
            resource: ResourceType.ACL,
            action: Action.EDIT,
          }}
        >
          <S.DeleteCell>
            <div className="show-on-hover">
              <DeleteIcon fill={theme.acl.table.deleteIcon} />
            </div>
          </S.DeleteCell>
        </ActionPermissionWrapper>
      );
    },
    size: 76,
  };
};

export {
  createPrincipalCell,
  createResourceCell,
  createPatternCell,
  createHostCell,
  createOperationCell,
  createParmissionCell,
  createActionsCell,
  createNameCell,
};
