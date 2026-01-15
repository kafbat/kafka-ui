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
  value: yup.string().required(),
  id: yup.string(),
});

export interface AddEditFilterContainerProps {
  closeSideBar: () => void;
  currentFilter?: AdvancedFilter;
  smartFilter?: AdvancedFilter | undefined;
  setSmartFilter: (filter: AdvancedFilter, persisted?: boolean) => void;
}

interface AddMessageFilters extends Omit<AdvancedFilter, 'filterCode'> {}

function codeToName(code: string) {
  if (code.length > 32) {
    return code.substring(0, 32);
  }
  return code;
}

function submitValidation(
  id: string,
  code: string,
  currentFilterId: string
): boolean {
  const filter = selectFilter(id)(useMessageFiltersStore.getState());

  if (id === '') {
    const name = codeToName(code);
    const filters = Object.keys(useMessageFiltersStore.getState().filters);

    if (filters.includes(name)) {
      showAlert('error', {
        id: '',
        title: 'Validation Error',
        message: `The name “${name}” already exists. Please enter a unique name.`,
      });
    }

    return true;
  }

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

  if (values.value.length > 32) {
    return codeToName(values.value);
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

  const methods = useForm<AddMessageFilters>({
    mode: 'onTouched',
    resolver: yupResolver(validationSchema, { context: ['name name'] }),
    context: {
      value: ['name'],
    },
    defaultValues: {
      id: currentFilter?.id,
      value: currentFilter?.value,
    },
  });
  const {
    handleSubmit,
    control,
    formState: { isDirty, isSubmitting, isValid, errors },
  } = methods;

  const { mutateAsync } = useRegisterSmartFilter({ clusterName, topicName });

  const onSubmit = async (values: AddMessageFilters) => {
    try {
      if (submitValidation(values.id, values.value, filterId)) {
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
          replace(filterId, filterValue);
          if (smartFilter?.id === filterId) {
            setSmartFilter(filterValue, isPersisted);
          }

          closeSideBar();
          return;
        }

        save(filterValue);
        setSmartFilter(filterValue);
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
          <QuestionInfo />
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
