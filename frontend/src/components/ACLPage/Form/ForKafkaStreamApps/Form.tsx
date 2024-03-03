import React, { FC } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { ClusterName } from 'redux/interfaces';
import { topicsPayload } from 'lib/fixtures/topics';
import { useCreateStreamAppAcl } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import Input from 'components/common/Input/Input';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import * as S from 'components/ACLPage/Form/Form.styled';
import { AclFormProps } from 'components/ACLPage/Form/types';

import { toRequest } from './lib';
import formSchema from './schema';
import { FormValues } from './types';

const ForKafkaStreamAppsForm: FC<AclFormProps> = ({ formRef, closeForm }) => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateStreamAppAcl(clusterName);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });

  const onSubmit = async (data: FormValues) => {
    try {
      const resource = toRequest(data);
      await create.createResource(resource);
      closeForm();
    } catch (e) {
      // exception
    }
  };

  const topics = topicsPayload.map((topic) => {
    return {
      label: topic.name,
      value: topic.name,
    };
  });

  return (
    <FormProvider {...methods}>
      <S.Form ref={formRef} onSubmit={methods.handleSubmit(onSubmit)}>
        <hr />
        <S.Field>
          <S.Label htmlFor="principal">Principal</S.Label>
          <Input name="principal" id="principal" withError />
        </S.Field>

        <S.Field>
          <S.Label htmlFor="host">Host restriction</S.Label>
          <Input name="host" id="host" withError />
        </S.Field>
        <hr />
        <S.Field>
          <S.Label>From topic(s)</S.Label>
          <ControlledMultiSelect name="topics" options={topics} />
        </S.Field>
        <S.Field>
          <S.Label>To topic(s)</S.Label>
          <ControlledMultiSelect name="topics" options={topics} />
        </S.Field>
        <S.Field>
          <S.Label>Application.id</S.Label>
          <Input name="applicationId" />
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default ForKafkaStreamAppsForm;
