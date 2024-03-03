import React, { FC, useContext } from 'react';
import { yupResolver } from '@hookform/resolvers/yup';
import { FormProvider, useForm } from 'react-hook-form';
import { useCreateCustomAcl } from 'lib/hooks/api/acl';
import ControlledRadio from 'components/common/Radio/ControlledRadio';
import Input from 'components/common/Input/Input';
import ControlledSelect from 'components/common/Select/ControlledSelect';
import { prefixOptions } from 'components/ACLPage/Form/constants';
import { AclFormProps } from 'components/ACLPage/Form//types';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterName } from 'redux/interfaces';
import * as S from 'components/ACLPage/Form/Form.styled';

import formSchema from './schema';
import { FormValues } from './types';
import { toFormValue, toRequest } from './lib';
import {
  defaultValues,
  operations,
  permissions,
  resourceTypes,
} from './constants';
import ACLFormContext from '../AclFormContext';

const CustomACLForm: FC<AclFormProps> = ({ formRef }) => {
  const { onClose: closeForm } = useContext(ACLFormContext);
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
      closeForm();
    } catch (e) {
      // error
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
            <ControlledRadio name="namePatternType" options={prefixOptions} />
            <Input name="resourceName" id="resourceName" withError />
          </S.ControlList>
        </S.Field>
      </S.Form>
    </FormProvider>
  );
};

export default React.memo(CustomACLForm);
