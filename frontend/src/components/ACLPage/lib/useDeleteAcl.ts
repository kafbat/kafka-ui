import { KafkaAcl } from 'generated-sources';
import { useDeleteAcl as useDeleteAclMutation } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import { useConfirm } from 'lib/hooks/useConfirm';
import { ClusterName } from 'lib/interfaces/cluster';

const useDeleteKafkaAcl = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const modal = useConfirm(true);
  const { deleteResource } = useDeleteAclMutation(clusterName);

  const deleteKafkaAcl = (acl: KafkaAcl | null) => {
    if (acl) {
      modal('Are you sure want to delete this ACL record?', () =>
        deleteResource(acl)
      );
    }
  };

  return {
    deleteKafkaAcl,
  };
};

export default useDeleteKafkaAcl;
