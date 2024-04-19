import React, { useEffect } from 'react';
import { ClusterSubjectParam } from 'lib/paths';
import useAppParams from 'lib/hooks/useAppParams';
import PageLoader from 'components/common/PageLoader/PageLoader';
import { useNavigate } from 'react-router-dom';
import { useGetLatestSchema } from 'lib/hooks/api/schemas';

import Form from './Form';

const Edit: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName, subject } = useAppParams<ClusterSubjectParam>();
  const {
    isFetching,
    isError,
    data: schema,
  } = useGetLatestSchema({
    clusterName,
    subject,
  });

  useEffect(() => {
    if (isError) {
      navigate('/404');
    }
  }, [isError]);

  if (isFetching) {
    return <PageLoader />;
  }

  if (!schema) return null;

  return <Form schema={schema} />;
};

export default Edit;
