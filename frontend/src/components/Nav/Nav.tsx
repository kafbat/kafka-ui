import { useClusters } from 'lib/hooks/api/clusters';
import React, { type FC } from 'react';

import * as S from './Nav.styled';
import MenuItem from './Menu/MenuItem';
import ClusterMenu from './ClusterMenu/ClusterMenu';

const Nav: FC = () => {
  const clusters = useClusters();

  return (
    <aside aria-label="Sidebar Menu">
      <S.List>
        <MenuItem variant="primary" to="/" title="Dashboard" />
      </S.List>
      {clusters.isSuccess &&
        clusters.data.map((cluster) => (
          <ClusterMenu
            key={cluster.name}
            name={cluster.name}
            status={cluster.status}
            features={cluster.features}
            singleMode={clusters.data.length === 1}
          />
        ))}
    </aside>
  );
};

export default Nav;
