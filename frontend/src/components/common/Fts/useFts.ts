import React from 'react';
import { useSearchParams } from 'react-router-dom';

type FtsAvailableResource = 'topics';

const storageName = 'kafbat-ui_fts';

const useFts = (resourceName: FtsAvailableResource) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const storageKey = `${storageName}:${resourceName}`;

  React.useEffect(() => {
    if (!!localStorage.getItem(storageKey) && !searchParams.has('fts')) {
      searchParams.set('fts', 'true');
    }
    setSearchParams(searchParams);
  }, []);

  const handleSwitch = () => {
    if (searchParams.has('fts')) {
      localStorage.removeItem(storageKey);
      searchParams.delete('fts');
    } else {
      localStorage.setItem(storageKey, 'true');
      searchParams.set('fts', 'true');
    }
    searchParams.set('page', '1');
    setSearchParams(searchParams);
  };

  return {
    handleSwitch,
    isFtsEnabled: !!searchParams.get('fts'),
  };
};

export default useFts;
