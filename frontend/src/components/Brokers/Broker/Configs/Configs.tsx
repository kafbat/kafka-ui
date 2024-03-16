import React, { type FC, useMemo, useState } from 'react';
import { ClusterBrokerParam } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import {
  useBrokerConfig,
  useUpdateBrokerConfigByName,
} from 'lib/hooks/api/brokers';
import Table from 'components/common/NewTable';
import type { BrokerConfig } from 'generated-sources';
import Search from 'components/common/Search/Search';
import {
  getBrokerConfigsTableColumns,
  getConfigTableData,
} from 'components/Brokers/Broker/Configs/lib/utils';

import * as S from './Configs.styled';

const Configs: FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const { clusterName, brokerId } = useAppParams<ClusterBrokerParam>();
  const { data: configs = [] } = useBrokerConfig(clusterName, Number(brokerId));
  const updateBrokerConfigByName = useUpdateBrokerConfigByName(
    clusterName,
    Number(brokerId)
  );

  const tableData = useMemo(
    () => getConfigTableData(configs, searchQuery),
    [configs, searchQuery]
  );

  const onUpdateInputCell = async (
    name: BrokerConfig['name'],
    value: BrokerConfig['value']
  ) =>
    updateBrokerConfigByName.mutateAsync({ name, brokerConfigItem: { value } });

  const columns = useMemo(
    () => getBrokerConfigsTableColumns(onUpdateInputCell),
    []
  );

  return (
    <>
      <S.SearchWrapper>
        <Search
          onChange={setSearchQuery}
          placeholder="Search by Key or Value"
          value={searchQuery}
        />
      </S.SearchWrapper>
      <Table columns={columns} data={tableData} />
    </>
  );
};

export default Configs;
