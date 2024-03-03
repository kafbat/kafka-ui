import { KafkaAcl, KafkaAclNamePatternType } from 'generated-sources';
import isRegex from 'lib/isRegex';
import { PrefixType } from 'components/ACLPage/Form/types';

import { FormValues } from './types';

export function toRequest(formValue: FormValues): KafkaAcl {
  let namePatternType: KafkaAclNamePatternType;
  if (formValue.namePatternType === PrefixType.PREFIXED) {
    namePatternType = KafkaAclNamePatternType.PREFIXED;
  } else if (isRegex(formValue.resourceName)) {
    namePatternType = KafkaAclNamePatternType.MATCH;
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

export function toFormValue(acl: KafkaAcl): FormValues {
  let namePatternType: PrefixType;
  if (acl.namePatternType === KafkaAclNamePatternType.PREFIXED) {
    namePatternType = PrefixType.PREFIXED;
  } else {
    namePatternType = PrefixType.EXACT;
  }

  return {
    resourceType: acl.resourceType,
    resourceName: acl.resourceName,
    principal: acl.principal,
    host: acl.host,
    operation: acl.operation,
    permission: acl.permission,
    namePatternType,
  };
}
