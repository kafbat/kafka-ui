import React from 'react';
import * as S from 'components/Topics/Topic/Messages/Filters/Filters.styled';
import { InputLabel } from 'components/common/Input/InputLabel.styled';
import Input from 'components/common/Input/Input';
import { FormProvider, Controller, useForm } from 'react-hook-form';
import { ErrorMessage } from '@hookform/error-message';
import { Button } from 'components/common/Button/Button';
import { FormError } from 'components/common/Input/Input.styled';
import Editor from 'components/common/Editor/Editor';
import { yupResolver } from '@hookform/resolvers/yup';
import yup from 'lib/yupExtended';
import { useRegisterSmartFilter } from 'lib/hooks/api/topicMessages';
import useAppParams from 'lib/hooks/useAppParams';
import { RouteParamsClusterTopic } from 'lib/paths';
import {
  AdvancedFilter,
  selectFilter,
  useMessageFiltersStore,
} from 'lib/hooks/useMessageFiltersStore';
import { showAlert } from 'lib/errorHandling';
import Flexbox from 'components/common/FlexBox/FlexBox';
import { useIsMessagesSmartFilterPersisted } from 'lib/hooks/useMessagesFilters';

import QuestionInfo from './QuestionInfo';

const validationSchema = yup.object().shape({
  saveFilter: yup.boolean(),
  value: yup.string().required(),
  id: yup.string().when('saveFilter', {
    is: (value: boolean | undefined) => typeof value === 'undefined' || value,
    then: (schema) => schema.required(),
    otherwise: (schema) => schema.notRequired(),
  }),
});

export interface AddEditFilterContainerProps {
  closeSideBar: () => void;
  currentFilter?: AdvancedFilter;
  smartFilter?: AdvancedFilter | undefined;
  setSmartFilter: (filter: AdvancedFilter, persisted?: boolean) => void;
}

interface AddMessageFilters extends Omit<AdvancedFilter, 'filterCode'> {
  saveFilter: boolean;
}

function submitValidation(id: string, currentFilterId: string): boolean {
  const filter = selectFilter(id)(useMessageFiltersStore.getState());

  if (filter && filter.id !== currentFilterId) {
    showAlert('error', {
      id: '',
      title: 'Validation Error',
      message: 'Filter with the same name already exists',
    });
    return true;
  }

  return false;
}

function getLabelName(values: AddMessageFilters) {
  if (values.id) {
    return values.id;
  }

  if (values.value.length > 24) {
    return values.value.substring(0, 10);
  }

  return values.value;
}

const AddEditFilterContainer: React.FC<AddEditFilterContainerProps> = ({
  closeSideBar,
  smartFilter,
  currentFilter,
  setSmartFilter,
}) => {
  const filterId = currentFilter?.id || '';
  const isEdit = !!currentFilter;
  const isPersisted = useIsMessagesSmartFilterPersisted();

  const { clusterName, topicName } = useAppParams<RouteParamsClusterTopic>();
  const save = useMessageFiltersStore((state) => state.save);
  const replace = useMessageFiltersStore((state) => state.replace);
  const commit = useMessageFiltersStore((state) => state.commit);

  const methods = useForm<AddMessageFilters>({
    mode: 'onChange',
    resolver: yupResolver(validationSchema),
    defaultValues: {
      id: currentFilter?.id,
      value: currentFilter?.value,
    },
  });
  const {
    handleSubmit,
    control,
    formState: { isDirty, isSubmitting, isValid, errors },
    reset,
  } = methods;

  const { mutateAsync } = useRegisterSmartFilter({ clusterName, topicName });

  const onSubmit = async (values: AddMessageFilters) => {
    try {
      if (submitValidation(values.id, filterId)) {
        return;
      }

      const messageFilter = await mutateAsync({ filterCode: values.value });
      if (messageFilter.id) {
        const filterValue = {
          ...values,
          id: getLabelName(values),
          filterCode: messageFilter.id,
        };

        if (isEdit) {
          if (isPersisted) {
            replace(filterId, filterValue);
          } else {
            // update the non persisted storage to pick up names
            commit(filterValue);
          }

          // when the active is the one that is getting edited
          if (smartFilter?.id === filterId) {
            setSmartFilter(filterValue, isPersisted);
          }

          closeSideBar();
          return;
        }

        if (values.saveFilter) {
          save(filterValue);
          reset({ id: '', value: '', saveFilter: false });
          return;
        }

        commit(filterValue);
        setSmartFilter(filterValue, false);
        closeSideBar();
      }
    } catch (e) {
      // do nothing
    }
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onSubmit)} aria-label="Filters submit Form">
        <div>
          <InputLabel>Filter code</InputLabel>
          <Controller
            control={control}
            name="value"
            render={({ field: { onChange, value } }) => (
              <Editor
                value={value}
                minLines={5}
                maxLines={28}
                onChange={onChange}
                setOptions={{
                  showLineNumbers: false,
                }}
              />
            )}
          />
        </div>
        <div>
          <FormError>
            <ErrorMessage errors={errors} name="value" />
          </FormError>
        </div>
        {!isEdit && (
          <InputLabel>
            <input {...methods.register('saveFilter')} type="checkbox" />
            Save this filter
          </InputLabel>
        )}
        <div>
          <InputLabel>Display name</InputLabel>
          <Input
            inputSize="M"
            placeholder="Enter Name"
            autoComplete="off"
            name="id"
          />
        </div>
        <div>
          <FormError>
            <ErrorMessage errors={errors} name="id" />
          </FormError>
        </div>
        <S.FilterButtonWrapper isEdit={isEdit}>
          {!isEdit && <QuestionInfo />}
          <Flexbox gap="10px">
            <Button
              buttonSize="M"
              buttonType="secondary"
              type="button"
              onClick={closeSideBar}
            >
              Cancel
            </Button>
            <Button
              buttonSize="M"
              buttonType="primary"
              type="submit"
              disabled={!isValid || isSubmitting || !isDirty}
            >
              {isEdit ? 'Edit Filter' : 'Add Filter'}
            </Button>
          </Flexbox>
        </S.FilterButtonWrapper>
      </form>
    </FormProvider>
  );
};

export default AddEditFilterContainer;
