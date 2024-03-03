import { CreateProducerAcl } from 'generated-sources/models/CreateProducerAcl';
import { Option } from 'react-multi-select-component';

import { FormValues } from './types';

const prepareOptions = (options: Option[]): string[] => {
  return options.map((opt) => opt.value);
};

export const toRequest = (formValues: FormValues): CreateProducerAcl => {
  return {
    principal: formValues.principal,
    host: formValues.host,
    topics: prepareOptions(formValues.topics),
    topicsPrefix: formValues.topicsPrefix,
    transactionalId: formValues.transactionalId,
    transactionsIdPrefix: formValues.transactionsIdPrefix,
    idempotent: formValues.indemponent,
  };
};
