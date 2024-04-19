import React, { FC, useContext } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { ClusterName } from 'lib/interfaces/cluster';
import { useCreateStreamAppAcl } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import Input from 'components/common/Input/Input';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import * as S from 'components/ACLPage/Form/Form.styled';
import { AclDetailedFormProps } from 'components/ACLPage/Form/types';
import useTopicsOptions from 'components/ACLPage/lib/useTopicsOptions';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';

import { toRequest } from './lib';
import formSchema from './schema';
import { FormValues } from './types';

const ForKafkaStreamAppsForm: FC<AclDetailedFormProps> = ({ formRef }) => {
  const context = useContext(ACLFormContext);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });

  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateStreamAppAcl(clusterName);
  const topics = useTopicsOptions(clusterName);

  const onSubmit = async (data: FormValues) => {
    try {
      const resource = toRequest(data);
      await create.createResource(resource);
      context?.close();
    } catch (e) {
      // no custom error
    }
  };

  return (
    <FormProvider {...methods}>
      <S.Form ref={formRef} onSubmit={methods.handleSubmit(onSubmit)}>
        <hr />
        <S.Field>
          <S.Label htmlFor="principal">Principal</S.Label>
          <Input
            name="principal"
            id="principal"
            placeholder="Principal"
            withError
          />
        </S.Field>

        <S.Field>
          <S.Label htmlFor="host">Host restriction</S.Label>
          <Input name="host" id="host" placeholder="Host" withError />
        </S.Field>
        <hr />
        <S.Field>
          <S.Label>From topic(s)</S.Label>
          <ControlledMultiSelect name="inputTopics" options={topics} />
        </S.Field>
        <S.Field>
          <S.Label>To topic(s)</S.Label>
          <ControlledMultiSelect name="outputTopics" options={topics} />
        </S.Field>
        <S.Field>
          <S.Label>Application.id</S.Label>
          <Input name="applicationId" placeholder="Application ID" />
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default ForKafkaStreamAppsForm;
