import { useConsumerGroups } from 'lib/hooks/api/consumers';
import { useMemo } from 'react';

const useConsumerGroupsOptions = (clusterName: string, search: string) => {
  const { data } = useConsumerGroups({ clusterName, search });
  const consumerGroups = useMemo(() => {
    return (
      data?.consumerGroups?.map((cg) => {
        return {
          value: cg.groupId,
          label: cg.groupId,
        };
      }) || []
    );
  }, [clusterName, search]);

  return consumerGroups;
};

export default useConsumerGroupsOptions;
