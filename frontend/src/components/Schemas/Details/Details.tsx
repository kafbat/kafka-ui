import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ClusterSubjectParam,
  clusterSchemaEditPageRelativePath,
  clusterSchemaSchemaComparePageRelativePath,
  clusterSchemasPath,
} from 'lib/paths';
import ClusterContext from 'components/contexts/ClusterContext';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { Button } from 'components/common/Button/Button';
import { TableTitle } from 'components/common/table/TableTitle/TableTitle.styled';
import useAppParams from 'lib/hooks/useAppParams';
import { Dropdown } from 'components/common/Dropdown';
import Table from 'components/common/NewTable';
import { Action, ResourceType } from 'generated-sources';
import {
  ActionButton,
  ActionDropdownItem,
} from 'components/common/ActionComponent';
import {
  useDeleteSchema,
  useGetLatestSchema,
  useGetSchemasVersions,
} from 'lib/hooks/api/schemas';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import ErrorPage from 'components/ErrorPage/ErrorPage';

import LatestVersionItem from './LatestVersion/LatestVersionItem';
import SchemaVersion from './SchemaVersion/SchemaVersion';

const Details: React.FC = () => {
  const navigate = useNavigate();
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName, subject } = useAppParams<ClusterSubjectParam>();
  const versions = useGetSchemasVersions({
    clusterName,
    subject,
  });
  const schema = useGetLatestSchema({
    clusterName,
    subject,
  });

  const { mutateAsync: deleteSchema } = useDeleteSchema({
    clusterName,
    subject,
  });

  const columns = React.useMemo(
    () => [
      { header: 'Version', accessorKey: 'version' },
      { header: 'ID', accessorKey: 'id' },
      { header: 'Type', accessorKey: 'schemaType' },
    ],
    []
  );

  const deleteHandler = async () => {
    await deleteSchema();
    navigate('../');
  };

  return (
    <>
      <ResourcePageHeading
        text={schema.data?.subject || subject}
        backText="Schema Registry"
        backTo={clusterSchemasPath(clusterName)}
      >
        <Button
          buttonSize="M"
          buttonType="primary"
          to={{
            pathname: clusterSchemaSchemaComparePageRelativePath,
            search: `leftVersion=${versions.data?.[0]?.version}&rightVersion=${versions.data?.[0]?.version}`,
          }}
        >
          Compare Versions
        </Button>
        {!isReadOnly && (
          <>
            <ActionButton
              buttonSize="M"
              buttonType="primary"
              to={clusterSchemaEditPageRelativePath}
              permission={{
                resource: ResourceType.SCHEMA,
                action: Action.EDIT,
                value: subject,
              }}
            >
              Edit Schema
            </ActionButton>
            <Dropdown>
              <ActionDropdownItem
                confirm={
                  <>
                    Are you sure want to remove <b>{subject}</b> schema?
                  </>
                }
                onClick={deleteHandler}
                danger
                permission={{
                  resource: ResourceType.SCHEMA,
                  action: Action.DELETE,
                  value: subject,
                }}
              >
                Remove schema
              </ActionDropdownItem>
            </Dropdown>
          </>
        )}
      </ResourcePageHeading>

      {(schema.isLoading || schema.isRefetching || versions.isLoading || versions.isRefetching) && <PageLoader />}

      {(schema.error || versions.error) && (
        <ErrorPage
          status={schema.error?.status || versions.error?.status}
          onClick={() => {
            schema.refetch();
            versions.refetch();
          }}
          resourceName={`Schema ${subject}`}
        />
      )}

      {schema.isSuccess && versions.isSuccess && (
        <>
          {schema.data && <LatestVersionItem schema={schema.data} />}
          <TableTitle>Old versions</TableTitle>
          <Table
            columns={columns}
            data={versions.data || []}
            getRowCanExpand={() => true}
            renderSubComponent={SchemaVersion}
            enableSorting
          />
        </>
      )}
    </>
  );
};

export default Details;
