import React from 'react';
import {
  ClusterNameRoute,
  clusterSchemaNewRelativePath,
  clusterSchemaPath,
} from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import { ActionButton } from 'components/common/ActionComponent';
import PageHeading from 'components/common/PageHeading/PageHeading';
import useAppParams from 'lib/hooks/useAppParams';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Search from 'components/common/Search/Search';
import PlusIcon from 'components/common/Icons/PlusIcon';
import Table, { LinkCell } from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';
import { Action, SchemaSubject, ResourceType } from 'generated-sources';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PER_PAGE } from 'lib/constants';
import { useGetSchemas } from 'lib/hooks/api/schemas';

import GlobalSchemaSelector from './GlobalSchemaSelector/GlobalSchemaSelector';

const List: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const {
    isFetching,
    isError,
    data = { pageCount: 1, schemas: [] as SchemaSubject[] },
  } = useGetSchemas({
    clusterName,
    page: Number(searchParams.get('page') || 1),
    perPage: Number(searchParams.get('perPage') || PER_PAGE),
    search: searchParams.get('q') || '',
  });

  const columns = React.useMemo<ColumnDef<SchemaSubject>[]>(
    () => [
      {
        header: 'Subject',
        accessorKey: 'subject',
        // eslint-disable-next-line react/no-unstable-nested-components
        cell: ({ getValue }) => (
          <LinkCell
            value={`${getValue<string | number>()}`}
            to={encodeURIComponent(`${getValue<string | number>()}`)}
          />
        ),
      },
      { header: 'Id', accessorKey: 'id' },
      { header: 'Type', accessorKey: 'schemaType' },
      { header: 'Version', accessorKey: 'version' },
      { header: 'Compatibility', accessorKey: 'compatibilityLevel' },
    ],
    []
  );

  return (
    <>
      <PageHeading clusterName={clusterName} text="Schema Registry">
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
      </PageHeading>
      <ControlPanelWrapper hasInput>
        <Search placeholder="Search by Schema Name" />
      </ControlPanelWrapper>
      {isFetching || isError ? (
        <PageLoader />
      ) : (
        <Table
          columns={columns}
          data={data.schemas || []}
          pageCount={data.pageCount || 1}
          emptyMessage="No schemas found"
          onRowClick={(row) =>
            navigate(clusterSchemaPath(clusterName, row.original.subject))
          }
          serverSideProcessing
        />
      )}
    </>
  );
};

export default List;
