import React, { FC, useContext } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { FormProvider, useForm } from 'react-hook-form';
import { useCreateCustomAcl } from 'lib/hooks/api/acl';
import ControlledRadio from 'components/common/Radio/ControlledRadio';
import Input from 'components/common/Input/Input';
import ControlledSelect from 'components/common/Select/ControlledSelect';
import { matchTypeOptions } from 'components/ACLPage/Form/constants';
import useAppParams from 'lib/hooks/useAppParams';
import * as S from 'components/ACLPage/Form/Form.styled';
import ACLFormContext from 'components/ACLPage/Form/AclFormContext';
import { AclDetailedFormProps } from 'components/ACLPage/Form/types';
import { ClusterName } from 'lib/interfaces/cluster';

import formSchema from './schema';
import { FormValues } from './types';
import { toRequest } from './lib';
import {
  defaultValues,
  operations,
  permissions,
  resourceTypes,
} from './constants';

const CustomACLForm: FC<AclDetailedFormProps> = ({ formRef }) => {
  const context = useContext(ACLFormContext);

  const methods = useForm<FormValues>({
    mode: 'all',
    resolver: yupResolver(formSchema),
    defaultValues: { ...defaultValues },
  });

  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const create = useCreateCustomAcl(clusterName);

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
          <S.Label htmlFor="resourceType">Resource type</S.Label>
          <ControlledSelect options={resourceTypes} name="resourceType" />
        </S.Field>

        <S.Field>
          <S.Label>Operations</S.Label>
          <S.ControlList>
            <ControlledRadio name="permission" options={permissions} />
            <ControlledSelect options={operations} name="operation" />
          </S.ControlList>
        </S.Field>

        <S.Field>
          <S.Field>Matching pattern</S.Field>
          <S.ControlList>
            <ControlledRadio
              name="namePatternType"
              options={matchTypeOptions}
            />
            <Input
              name="resourceName"
              id="resourceName"
              placeholder="Matching pattern"
              withError
            />
          </S.ControlList>
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default React.memo(CustomACLForm);
