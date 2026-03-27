import React, { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ColumnDef } from '@tanstack/react-table';
import useAppParams from 'lib/hooks/useAppParams';
import { useAcls } from 'lib/hooks/api/acl';
import { ClusterName } from 'lib/interfaces/cluster';
import { Action, KafkaAcl, ResourceType } from 'generated-sources';
import useBoolean from 'lib/hooks/useBoolean';
import ACLForm from 'components/ACLPage/Form/Form';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import PlusIcon from 'components/common/Icons/PlusIcon';
import ActionButton from 'components/common/ActionComponent/ActionButton/ActionButton';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Search from 'components/common/Search/Search';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import useFts from 'components/common/Fts/useFts';
import Fts from 'components/common/Fts/Fts';
import ClusterContext from 'components/contexts/ClusterContext';
import { theme } from 'theme/theme';
import AclsTable from 'components/ACLPage/Table/Table';
import {
  createPrincipalCell,
  createResourceCell,
  createOperationCell,
  createParmissionCell,
  createHostCell,
  createActionsCell,
  createPatternCell,
} from 'components/ACLPage/Table/TableCells';
import useDeleteKafkaAcl from 'components/ACLPage/lib/useDeleteAcl';
import PageLoader from 'components/common/PageLoader/PageLoader';
import ErrorPage from 'components/ErrorPage/ErrorPage';

import * as S from './List.styled';

const EMPTY_ACLS: KafkaAcl[] = [];
const ACList: React.FC = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('acl');
  const acls = useAcls({
    clusterName,
    search: searchParams.get('q') ?? '',
    fts: isFtsEnabled,
  });
  const { isReadOnly } = React.useContext(ClusterContext);
  const {
    value: isFormOpen,
    setFalse: closeForm,
    setTrue: openFrom,
  } = useBoolean();

  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (searchParams.get('q')) {
      params.set('page', '1'); // reset to first page on new search
    } else {
      params.delete('q');
    }
    setSearchParams(params, { replace: true });
  }, [searchParams.get('q')]);

  const { deleteKafkaAcl } = useDeleteKafkaAcl();
  const columns = React.useMemo<ColumnDef<KafkaAcl>[]>(() => {
    return [
      createPrincipalCell(),
      createResourceCell(),
      createPatternCell(),
      createHostCell(),
      createOperationCell(),
      createParmissionCell(),
      createActionsCell({ theme, onDelete: deleteKafkaAcl }),
    ];
  }, [theme]);

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

      {(acls.isLoading || acls.isRefetching) && <PageLoader offsetY={300} />}

      {acls.error && (
        <ErrorPage
          offsetY={300}
          status={acls.error.status}
          onClick={acls.refetch}
          text={acls.error.message}
        />
      )}

      {acls.isSuccess && (
        <AclsTable acls={acls.data ?? EMPTY_ACLS} columns={columns} />
      )}

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
