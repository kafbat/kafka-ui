import React, { useEffect, useState } from 'react';
import { Connect, FullConnectorInfo } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';

const ClusterVersionCell: React.FC<CellContext<Connect, FullConnectorInfo>> = ({ row }) => {
  const { address } = row.original;
  const [version, setVersion] = useState<string>('Loading...');

  useEffect(() => {
    if (!address) {
      setVersion('Unknown');
    } else {
      fetch(address)
        .then((response) => response.json())
        .then((data) => {
          setVersion(data.version || 'Unknown');
        })
        .catch((e) => {
          console.error(e);
          setVersion('?');
        });
    }
  }, [address]);

  return <>{version}</>;
};

export default ClusterVersionCell;
