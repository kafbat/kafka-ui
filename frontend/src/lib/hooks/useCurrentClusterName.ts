import { useLocation } from 'react-router-dom';

function useCurrentClusterName() {
  const location = useLocation();
  const parts = location.pathname.split('/');
  const clusterPosition = parts.indexOf('clusters');

  if (clusterPosition === undefined) {
    return undefined;
  }

  return parts[clusterPosition + 1];
}

export default useCurrentClusterName;
