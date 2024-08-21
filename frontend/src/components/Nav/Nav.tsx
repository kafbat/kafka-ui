import React, { FC, useState } from 'react';
import { useClusters } from 'lib/hooks/api/clusters';

import * as S from './Nav.styled';
import MenuItem from './Menu/MenuItem';
import ClusterMenu from './ClusterMenu/ClusterMenu';

const Nav: FC = () => {
  const [openTab, setOpenTab] = useState<string | false>(false);
  const { isSuccess, data: clusters } = useClusters();

  const handleTabChange = (tabName: string) => {
    setOpenTab((prev) => (prev === tabName ? false : tabName));
  };

  return (
    <aside aria-label="Sidebar Menu">
      <S.List>
        <MenuItem variant="primary" to="/" title="Dashboard" />
      </S.List>
      {isSuccess &&
        clusters.map((cluster) => (
          <ClusterMenu
            key={cluster.name}
            name={cluster.name}
            status={cluster.status}
            features={cluster.features}
            openTab={openTab}
            onTabClick={() => handleTabChange(cluster.name)}
          />
        ))}
    </aside>
  );
};

export default Nav;
