import { CreateProducerAcl } from 'generated-sources/models/CreateProducerAcl';

import { FormValues } from './types';

export const toRequest = (formValues: FormValues): CreateProducerAcl => {
  return {
    principal: formValues.principal,
    host: formValues.host,
    topics: formValues.topics?.map((opt) => opt.value),
    topicsPrefix: formValues.topicsPrefix,
    transactionalId: formValues.transactionalId,
    transactionsIdPrefix: formValues.transactionsIdPrefix,
    idempotent: formValues.idempotent,
  };
};
