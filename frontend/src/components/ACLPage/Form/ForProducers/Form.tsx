import React, { FC } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { useCreateProducerAcl } from 'lib/hooks/api/acl';
import { FormProvider, useForm } from 'react-hook-form';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterName } from 'redux/interfaces';
import { topicsPayload } from 'lib/fixtures/topics';
import Input from 'components/common/Input/Input';
import ControlledRadio from 'components/common/Radio/ControlledRadio';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import Checkbox from 'components/common/Checkbox/Checkbox';
import * as S from 'components/ACLPage/Form/Form.styled';
import { prefixOptions } from 'components/ACLPage/Form/constants';
import { AclFormProps } from 'components/ACLPage/Form/types';

import { toRequest } from './lib';
import { FormValues } from './types';
import formSchema from './schema';

const ForProducersForm: FC<AclFormProps> = ({ formRef, closeForm }) => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateProducerAcl(clusterName);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });

  const topics = topicsPayload.map((topic) => {
    return {
      label: topic.name,
      value: topic.name,
    };
  });

  const onSubmit = async (data: FormValues) => {
    try {
      await create.createResource(toRequest(data));
      closeForm();
    } catch (e) {
      // exception
    }
  };

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
          <S.Label>To Topic(s)</S.Label>
          <S.ControlList>
            <ControlledRadio name="topicsPrefix" options={prefixOptions} />
            <ControlledMultiSelect name="topics" options={topics} />
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Transaction ID</S.Field>
          <S.ControlList>
            <ControlledRadio
              name="transactionsIdPrefix"
              options={prefixOptions}
            />
            <Input name="transactionalId" id="transactionalId" />
          </S.ControlList>
        </S.Field>
        <hr />
        <Checkbox
          name="idempotent"
          label="Idempotent"
          hint="Check it if using enable idempotence=true"
        />
      </S.Form>
    </FormProvider>
  );
};

export default ForProducersForm;
