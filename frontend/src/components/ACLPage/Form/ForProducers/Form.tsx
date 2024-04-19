import React, { FC, useContext } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { useCreateProducerAcl } from 'lib/hooks/api/acl';
import { FormProvider, useForm } from 'react-hook-form';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterName } from 'lib/interfaces/cluster';
import Input from 'components/common/Input/Input';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import Checkbox from 'components/common/Checkbox/Checkbox';
import * as S from 'components/ACLPage/Form/Form.styled';
import { AclDetailedFormProps, MatchType } from 'components/ACLPage/Form/types';
import useTopicsOptions from 'components/ACLPage/lib/useTopicsOptions';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import MatchTypeSelector from 'components/ACLPage/Form/components/MatchTypeSelector';

import { toRequest } from './lib';
import { FormValues } from './types';
import formSchema from './schema';

const ForProducersForm: FC<AclDetailedFormProps> = ({ formRef }) => {
  const context = useContext(ACLFormContext);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });
  const { setValue } = methods;

  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateProducerAcl(clusterName);
  const topics = useTopicsOptions(clusterName);

  const onTopicTypeChange = (value: string) => {
    if (value === MatchType.EXACT) {
      setValue('topicsPrefix', undefined);
    } else {
      setValue('topics', undefined);
    }
  };

  const onTransactionIdTypeChange = (value: string) => {
    if (value === MatchType.EXACT) {
      setValue('transactionsIdPrefix', undefined);
    } else {
      setValue('transactionalId', undefined);
    }
  };

  const onSubmit = async (data: FormValues) => {
    try {
      await create.createResource(toRequest(data));
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
          <S.Label>To Topic(s)</S.Label>
          <S.ControlList>
            <MatchTypeSelector
              exact={<ControlledMultiSelect name="topics" options={topics} />}
              prefixed={<Input name="topicsPrefix" placeholder="Prefix..." />}
              onChange={onTopicTypeChange}
            />
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Transaction ID</S.Field>
          <S.ControlList>
            <MatchTypeSelector
              exact={
                <Input name="transactionalId" placeholder="Transactional ID" />
              }
              prefixed={
                <Input name="transactionsIdPrefix" placeholder="Prefix..." />
              }
              onChange={onTransactionIdTypeChange}
            />
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
