import React from 'react';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { FormProvider, useForm } from 'react-hook-form';

import * as S from './BasicSignIn.styled';

interface FormValues {
  username: string;
  password: string;
}

function BasicSignIn() {
  const methods = useForm<FormValues>();

  return (
    <FormProvider {...methods}>
      <S.Form style={{ width: '100%' }}>
        <S.Fieldset style={{ width: '100%' }}>
          <S.Field>
            <S.Label htmlFor="username">Username</S.Label>
            <Input
              name="username"
              id="username"
              placeholder="Enter your username"
              style={{ borderRadius: '8px' }}
            />
          </S.Field>
          <S.Field>
            <S.Label htmlFor="password">Password</S.Label>
            <Input
              name="password"
              id="password"
              placeholder="Enter your password"
              style={{ borderRadius: '8px' }}
            />
          </S.Field>
        </S.Fieldset>
        <Button
          buttonSize="L"
          buttonType="primary"
          onClick={() => console.log('click')}
          style={{ width: '100%', borderRadius: '8px' }}
        >
          Log in
        </Button>
      </S.Form>
    </FormProvider>
  );
}

export default BasicSignIn;
