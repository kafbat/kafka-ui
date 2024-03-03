import React, { FC, useContext, useState } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { FormProvider, useForm } from 'react-hook-form';
import { ClusterName } from 'redux/interfaces';
import { useCreateConsumersAcl } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import Input from 'components/common/Input/Input';
import * as S from 'components/ACLPage/Form/Form.styled';
import { prefixOptions } from 'components/ACLPage/Form/constants';
import { AclFormProps, PrefixType } from 'components/ACLPage/Form/types';

import { FormValues } from './types';
import { toRequest } from './lib';
import formSchema from './schema';
import Radio from 'components/common/Radio/Radio';
import useTopicsOptions from 'components/ACLPage/lib/useTopicsOptions';
import useConsumerGroupsOptions from 'components/ACLPage/lib/useConsumerGroupsOptions';
import ACLFormContext from '../AclFormContext';

const ForConsumersForm: FC<AclFormProps> = ({ formRef }) => {
  const { onClose: closeForm } = useContext(ACLFormContext);
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateConsumersAcl(clusterName);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });

  const { setValue } = methods;

  const onSubmit = async (data: FormValues) => {
    try {
      await create.createResource(toRequest(data));
      closeForm();
    } catch (e) {
      console.error(e);
      // exception handle
    }
  };

  const topics = useTopicsOptions(clusterName);
  const consumerGroups = useConsumerGroupsOptions(clusterName, '');

  const [topicType, setTopicType] = useState(PrefixType.EXACT);
  const onTopicTypeChange = (value: string) => {
    if (value == PrefixType.EXACT) {
      setValue('topicsPrefix', undefined);
    } else {
      setValue('topics', undefined);
    }
    setTopicType(value as PrefixType);
  };

  const [cgType, setCgType] = useState(PrefixType.EXACT);
  const onCgTypeChange = (value: string) => {
    if (value == PrefixType.EXACT) {
      setValue('consumerGroupsPrefix', undefined);
    } else {
      setValue('consumerGroups', undefined);
    }
    setCgType(value as PrefixType);
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
          <S.Label>From Topic(s)</S.Label>
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
          <S.Field>Consumer group(s)</S.Field>
          <S.ControlList>
            <Radio
              value={cgType}
              options={prefixOptions}
              onChange={onCgTypeChange}
            />
            {cgType === PrefixType.EXACT ? (
              <ControlledMultiSelect
                name="consumerGroups"
                options={consumerGroups}
              />
            ) : (
              <Input
                name="consumerGroupsPrefix"
                placeholder="Prefix..."
              ></Input>
            )}
          </S.ControlList>
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default React.memo(ForConsumersForm);
