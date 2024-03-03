import React, { FC, useContext, useState } from 'react';
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
import { AclFormProps, PrefixType } from 'components/ACLPage/Form/types';

import { toRequest } from './lib';
import { FormValues } from './types';
import formSchema from './schema';
import Radio from 'components/common/Radio/Radio';
import useTopicsOptions from 'components/ACLPage/lib/useTopicsOptions';
import ACLFormContext from '../AclFormContext';

const ForProducersForm: FC<AclFormProps> = ({ formRef }) => {
  const { onClose: closeForm } = useContext(ACLFormContext);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });
  const { setValue } = methods;

  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateProducerAcl(clusterName);
  const topics = useTopicsOptions(clusterName);
  const [topicType, setTopicType] = useState(PrefixType.EXACT);
  const [transactionIdType, setTransactionIdType] = useState(PrefixType.EXACT);

  const onTopicTypeChange = (value: string) => {
    if (value == PrefixType.EXACT) {
      setValue('topicsPrefix', undefined);
    } else {
      setValue('topics', undefined);
    }
    setTopicType(value as PrefixType);
  };

  const onTransactionIdTypeChange = (value: string) => {
    if (value == PrefixType.EXACT) {
      setValue('transactionsIdPrefix', undefined);
    } else {
      setValue('transactionalId', undefined);
    }
    setTransactionIdType(value as PrefixType);
  };

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
            <Radio
              value={topicType}
              options={prefixOptions}
              onChange={onTopicTypeChange}
            />
            {topicType === PrefixType.EXACT ? (
              <ControlledMultiSelect name="topics" options={topics} />
            ) : (
              <Input name="topicsPrefix" placeholder="Prefix..."></Input>
            )}
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Transaction ID</S.Field>
          <S.ControlList>
            <Radio
              value={transactionIdType}
              options={prefixOptions}
              onChange={onTransactionIdTypeChange}
            />
            {transactionIdType === PrefixType.EXACT ? (
              <Input name="transactionalId" />
            ) : (
              <Input name="transactionsIdPrefix" />
            )}
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
