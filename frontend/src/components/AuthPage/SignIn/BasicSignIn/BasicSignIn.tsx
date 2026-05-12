import React from 'react';
import { Button } from 'components/common/Button/Button';
import Input from 'components/common/Input/Input';
import { Controller, FormProvider, useForm } from 'react-hook-form';
import { useAuthenticate } from 'lib/hooks/api/appConfig';
import AlertIcon from 'components/common/Icons/AlertIcon';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

import * as S from './BasicSignIn.styled';

interface FormValues {
  username: string;
  password: string;
}

function BasicSignIn() {
  const methods = useForm<FormValues>({
    defaultValues: { username: '', password: '' },
  });
  const navigate = useNavigate();
  const { mutateAsync, isPending } = useAuthenticate();
  const client = useQueryClient();

  const onSubmit = async (data: FormValues) => {
    await mutateAsync(data, {
      onSuccess: async (response) => {
        if (response.raw.url.includes('error')) {
          methods.setError('root', { message: 'error' });
        } else {
          await client.invalidateQueries({ queryKey: ['app', 'info'] });
          navigate('/');
        }
      },
    });
  };

  return (
    <FormProvider {...methods}>
      <S.Form onSubmit={methods.handleSubmit(onSubmit)}>
        <S.Fieldset>
          {methods.formState.errors.root && (
            <S.ErrorMessage>
              <AlertIcon />
              <S.ErrorMessageText>
                Username or password entered incorrectly
              </S.ErrorMessageText>
            </S.ErrorMessage>
          )}
          <Controller
            name="username"
            control={methods.control}
            render={({ field }) => (
              <S.Field>
                <S.Label htmlFor={field.name}>Username</S.Label>
                <Input
                  onChange={field.onChange}
                  value={field.value}
                  name={field.name}
                  id={field.name}
                  placeholder="Enter your username"
                  style={{ borderRadius: '8px' }}
                />
              </S.Field>
            )}
          />
          <Controller
            name="password"
            control={methods.control}
            render={({ field }) => (
              <S.Field>
                <S.Label htmlFor={field.name}>Password</S.Label>
                <Input
                  onChange={field.onChange}
                  value={field.value}
                  name={field.name}
                  type="password"
                  id={field.name}
                  placeholder="Enter your password"
                  style={{ borderRadius: '8px' }}
                />
              </S.Field>
            )}
          />
        </S.Fieldset>
        <Button
          buttonSize="L"
          buttonType="primary"
          type="submit"
          style={{ width: '100%', borderRadius: '8px' }}
          disabled={!methods.formState.isValid}
          inProgress={isPending}
        >
          {!isPending && 'Log in'}
        </Button>
      </S.Form>
    </FormProvider>
  );
}

export default BasicSignIn;
