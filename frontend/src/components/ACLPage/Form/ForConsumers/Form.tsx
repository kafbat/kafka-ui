import React, { FC } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { FormProvider, useForm } from 'react-hook-form';
import { ClusterName } from 'redux/interfaces';
import { consumerGroupPayload } from 'lib/fixtures/consumerGroups';
import { topicsPayload } from 'lib/fixtures/topics';
import { useCreateConsumersAcl } from 'lib/hooks/api/acl';
import useAppParams from 'lib/hooks/useAppParams';
import ControlledMultiSelect from 'components/common/MultiSelect/ControlledMultiSelect';
import Input from 'components/common/Input/Input';
import ControlledRadio from 'components/common/Radio/ControlledRadio';
import * as S from 'components/ACLPage/Form/Form.styled';
import { prefixOptions } from 'components/ACLPage/Form/constants';
import { AclFormProps } from 'components/ACLPage/Form/types';

import { FormValues } from './types';
import { toRequest } from './lib';
import formSchema from './schema';

const ForConsumersForm: FC<AclFormProps> = ({ formRef, closeForm }) => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateConsumersAcl(clusterName);
  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
  });

  const onSubmit = async (data: FormValues) => {
    try {
      await create.createResource(toRequest(data));
      closeForm();
    } catch (e) {
      // exception handle
    }
  };

  // const { data } = useTopics({ clusterName }); // TODO: exclude internal
  const topics = topicsPayload.map((topic) => {
    return {
      label: topic.name,
      value: topic.name,
    };
  });
  // const consumers = useConsumerGroups({ clusterName, search: '' }); // TODO: WTF
  const consumerGroups = [consumerGroupPayload].map((cg) => {
    return {
      value: cg.groupId,
      label: cg.groupId,
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
          <S.Label>From Topic(s)</S.Label>
          <S.ControlList>
            <ControlledRadio name="topicsPrefix" options={prefixOptions} />
            <ControlledMultiSelect name="topics" options={topics} />
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Consumer group(s)</S.Field>
          <S.ControlList>
            <ControlledRadio
              name="consumerGroupsPrefix"
              options={prefixOptions}
            />
            <ControlledMultiSelect
              name="consumerGroups"
              options={consumerGroups}
            />
          </S.ControlList>
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default React.memo(ForConsumersForm);
