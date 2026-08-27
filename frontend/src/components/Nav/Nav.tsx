import React, { type FC, useState } from 'react';
import { useClusters } from 'lib/hooks/api/clusters';
import useCurrentClusterName from 'lib/hooks/useCurrentClusterName';
import Search from 'components/common/Search/Search';

import * as S from './Nav.styled';
import MenuItem from './Menu/MenuItem';
import ClusterMenu from './ClusterMenu/ClusterMenu';

const Nav: FC = () => {
  const clusters = useClusters();
  const clusterName = useCurrentClusterName();
  const [search, setSearch] = useState('');

  const filteredClusters =
    clusters.isSuccess && search.trim()
      ? clusters.data.filter((c) =>
          c.name.toLowerCase().includes(search.toLowerCase())
        )
      : clusters.data;

  return (
    <aside aria-label="Sidebar Menu">
      <S.List>
        <MenuItem variant="primary" to="/" title="Dashboard" />
      </S.List>
      {clusters.isSuccess && clusters.data.length > 0 && (
        <S.SearchWrapper>
          <Search
            placeholder="Search clusters..."
            value={search}
            onChange={setSearch}
          />
        </S.SearchWrapper>
      )}
      {clusters.isSuccess &&
        filteredClusters?.map((cluster) => (
          <ClusterMenu
            key={cluster.name}
            name={cluster.name}
            status={cluster.status}
            features={cluster.features}
            opened={clusters.data.length === 1 || cluster.name === clusterName}
          />
        ))}
    </aside>
  );
};

export default Nav;
