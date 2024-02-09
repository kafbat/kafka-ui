import { Params, useParams } from 'react-router-dom';
import { ClusterNameRoute } from 'lib/paths';

export default function useAppParams<
  T extends { [K in keyof Params]?: string }
>() {
  const params = useParams<T>() as T;
  
  const hasClusterName = (params: T): params is T & ClusterNameRoute => typeof params.clusterName !== "undefined"
  if (hasClusterName(params)) {
    params.clusterName = decodeURIComponent(params.clusterName)
  }
  
  return params
}
