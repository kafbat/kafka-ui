import React from 'react';
import {
  ClusterNameRoute,
  clusterSchemaNewRelativePath,
  clusterSchemaPath,
} from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import { ActionButton } from 'components/common/ActionComponent';
import useAppParams from 'lib/hooks/useAppParams';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Search from 'components/common/Search/Search';
import PlusIcon from 'components/common/Icons/PlusIcon';
import Table, { LinkCell } from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';
import {
  Action,
  SchemaSubject,
  ResourceType,
  SchemaColumnsToSort,
  SortOrder,
} from 'generated-sources';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PER_PAGE } from 'lib/constants';
import { useGetSchemas } from 'lib/hooks/api/schemas';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import useFts from 'components/common/Fts/useFts';
import Fts from 'components/common/Fts/Fts';
import ErrorPage from 'components/ErrorPage/ErrorPage';

import GlobalSchemaSelector from './GlobalSchemaSelector/GlobalSchemaSelector';

const List: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('schemas');
  const schemas = useGetSchemas({
    clusterName,
    page: Number(searchParams.get('page') || 1),
    perPage: Number(searchParams.get('perPage') || PER_PAGE),
    search: searchParams.get('q') || '',
    orderBy: (searchParams.get('sortBy') as SchemaColumnsToSort) ?? undefined,
    sortOrder:
      (searchParams.get('sortDirection')?.toUpperCase() as SortOrder) ||
      undefined,
    fts: isFtsEnabled,
  });

  const columns = React.useMemo<ColumnDef<SchemaSubject>[]>(
    () => [
      {
        id: SchemaColumnsToSort.SUBJECT,
        header: 'Subject',
        accessorKey: 'subject',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <LinkCell
            wordBreak
            value={`${getValue<string | number>()}`}
            to={encodeURIComponent(`${getValue<string | number>()}`)}
          />
        ),
      },
      {
        id: SchemaColumnsToSort.ID,
        header: 'Id',
        accessorKey: 'id',
        size: 120,
      },
      {
        id: SchemaColumnsToSort.TYPE,
        header: 'Type',
        accessorKey: 'schemaType',
        size: 120,
      },
      {
        id: SchemaColumnsToSort.VERSION,
        header: 'Version',
        accessorKey: 'version',
        size: 120,
      },
      {
        id: SchemaColumnsToSort.COMPATIBILITY,
        header: 'Compatibility',
        accessorKey: 'compatibilityLevel',
        size: 160,
      },
    ],
    []
  );

  return (
    <>
      <ResourcePageHeading text="Schema Registry">
        {!isReadOnly && (
          <>
            <GlobalSchemaSelector />
            <ActionButton
              buttonSize="M"
              buttonType="primary"
              to={clusterSchemaNewRelativePath}
              permission={{
                resource: ResourceType.SCHEMA,
                action: Action.CREATE,
              }}
            >
              <PlusIcon /> Create Schema
            </ActionButton>
          </>
        )}
      </ResourcePageHeading>
      <ControlPanelWrapper hasInput>
        <Search
          key={clusterName}
          placeholder="Search by Schema Name"
          extraActions={<Fts resourceName="schemas" />}
        />
      </ControlPanelWrapper>

      {(schemas.isLoading || schemas.isRefetching) && (
        <PageLoader offsetY={300} />
      )}

      {schemas.error && (
        <ErrorPage
          offsetY={300}
          status={schemas.error.status}
          onClick={schemas.refetch}
          text={schemas.error.message}
        />
      )}

      {schemas.isSuccess && (
        <Table
          columns={columns}
          data={schemas.data?.schemas || []}
          pageCount={schemas.data?.pageCount || 1}
          emptyMessage="No schemas found"
          onRowClick={(row) =>
            navigate(clusterSchemaPath(clusterName, row.original.subject))
          }
          enableSorting
          serverSideProcessing
        />
      )}
    </>
  );
};

export default List;
