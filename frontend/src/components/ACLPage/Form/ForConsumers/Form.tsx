import React, { FC, useContext } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { FormProvider, useForm } from 'react-hook-form';
import { ClusterName } from 'lib/interfaces/cluster';
import { useCreateConsumersAcl } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import Input from 'components/common/Input/Input';
import * as S from 'components/ACLPage/Form/Form.styled';
import { AclDetailedFormProps, MatchType } from 'components/ACLPage/Form/types';
import useTopicsOptions from 'components/ACLPage/lib/useTopicsOptions';
import useConsumerGroupsOptions from 'components/ACLPage/lib/useConsumerGroupsOptions';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import MatchTypeSelector from 'components/ACLPage/Form/components/MatchTypeSelector';

import formSchema from './schema';
import { toRequest } from './lib';
import { FormValues } from './types';

const ForConsumersForm: FC<AclDetailedFormProps> = ({ formRef }) => {
  const context = useContext(ACLFormContext);
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
      context?.close();
    } catch (e) {
      // no custom error
    }
  };

  const topics = useTopicsOptions(clusterName);
  const consumerGroups = useConsumerGroupsOptions(clusterName);

  const onTopicTypeChange = (value: string) => {
    if (value === MatchType.EXACT) {
      setValue('topicsPrefix', undefined);
    } else {
      setValue('topics', undefined);
    }
  };

  const onConsumerGroupTypeChange = (value: string) => {
    if (value === MatchType.EXACT) {
      setValue('consumerGroupsPrefix', undefined);
    } else {
      setValue('consumerGroups', undefined);
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
          <S.Label>From Topic(s)</S.Label>
          <S.ControlList>
            <MatchTypeSelector
              exact={<ControlledMultiSelect name="topics" options={topics} />}
              prefixed={<Input name="topicsPrefix" placeholder="Prefix..." />}
              onChange={onTopicTypeChange}
            />
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Consumer group(s)</S.Field>
          <S.ControlList>
            <MatchTypeSelector
              exact={
                <ControlledMultiSelect
                  name="consumerGroups"
                  options={consumerGroups}
                />
              }
              prefixed={
                <Input name="consumerGroupsPrefix" placeholder="Prefix..." />
              }
              onChange={onConsumerGroupTypeChange}
            />
          </S.ControlList>
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default React.memo(ForConsumersForm);
