import { CreateConsumerAcl } from 'generated-sources/models/CreateConsumerAcl';

import { FormValues } from './types';

export const toRequest = (formValues: FormValues): CreateConsumerAcl => {
  return {
    principal: formValues.principal,
    host: formValues.host,
    consumerGroups: formValues.consumerGroups?.map((opt) => opt.value),
    consumerGroupsPrefix: formValues.consumerGroupsPrefix,
    topics: formValues.topics?.map((opt) => opt.value),
    topicsPrefix: formValues.topicsPrefix,
  };
};
