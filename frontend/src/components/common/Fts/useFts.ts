import ClusterContext from 'components/contexts/ClusterContext';
import React from 'react';
import { useSearchParams } from 'react-router-dom';

export type FtsAvailableResource =
  | 'topics'
  | 'acl'
  | 'consumer_groups'
  | 'connects'
  | 'schemas';

const storageName = 'kafbat-ui_fts';

const useFts = (resourceName: FtsAvailableResource) => {
  const { ftsEnabled: isFtsFeatureEnabled, ftsDefaultEnabled } =
    React.useContext(ClusterContext);
  const [searchParams, setSearchParams] = useSearchParams();
  const storageKey = `${storageName}:${resourceName}`;

  React.useEffect(() => {
    if (!isFtsFeatureEnabled) {
      searchParams.delete('fts');
      localStorage.removeItem(storageKey);
      setSearchParams(searchParams);
      return;
    }

    if (!searchParams.has('fts')) {
      const value = localStorage.getItem(storageKey);
      if (value === null) {
        searchParams.set('fts', ftsDefaultEnabled ? 'true' : 'false');
      } else {
        searchParams.set('fts', value);
      }
      setSearchParams(searchParams);
    }
  }, [isFtsFeatureEnabled, ftsDefaultEnabled, searchParams, storageKey]);

  const handleSwitch = () => {
    if (!isFtsFeatureEnabled) {
      return;
    }
    const currentValue = searchParams.get('fts') === 'true';
    if (currentValue) {
      localStorage.setItem(storageKey, 'false');
      searchParams.set('fts', 'false');
    } else {
      localStorage.setItem(storageKey, 'true');
      searchParams.set('fts', 'true');
    }

    searchParams.set('page', '1');
    setSearchParams(searchParams);
  };

  let isFtsEnabled = isFtsFeatureEnabled;
  if (isFtsEnabled) {
    if (searchParams.has('fts')) {
      isFtsEnabled = searchParams.get('fts') === 'true';
    } else if (localStorage.getItem(storageKey)) {
      isFtsEnabled = localStorage.getItem(storageKey) === 'true';
    } else {
      isFtsEnabled = ftsDefaultEnabled;
    }
  }

  return {
    handleSwitch,
    isFtsFeatureEnabled,
    isFtsEnabled,
  };
};

export default useFts;
