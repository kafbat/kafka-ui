import { KafkaAcl, KafkaAclNamePatternType } from 'generated-sources';
import { MatchType } from 'components/ACLPage/Form/types';

import { FormValues } from './types';

export function toRequest(formValue: FormValues): KafkaAcl {
  let namePatternType: KafkaAclNamePatternType;
  if (formValue.namePatternType === MatchType.PREFIXED) {
    namePatternType = KafkaAclNamePatternType.PREFIXED;
  } else {
    namePatternType = KafkaAclNamePatternType.LITERAL;
  }

  return {
    resourceType: formValue.resourceType,
    resourceName: formValue.resourceName,
    namePatternType,
    principal: formValue.principal,
    host: formValue.host,
    operation: formValue.operation,
    permission: formValue.permission,
  };
}
