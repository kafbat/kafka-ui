import { CreateStreamAppAcl } from 'generated-sources/models/CreateStreamAppAcl';

import { FormValues } from './types';

export const toRequest = (formValues: FormValues): CreateStreamAppAcl => {
  return {
    principal: formValues.principal,
    host: formValues.host,
    inputTopics: formValues.inputTopics.map((opt) => opt.value),
    outputTopics: formValues.outputTopics.map((opt) => opt.value),
    applicationId: formValues.applicationId,
  };
};
