import { SelectOption } from 'components/common/Select/Select';
import {
  KafkaAclOperationEnum,
  KafkaAclPermissionEnum,
  KafkaAclResourceType,
} from 'generated-sources';
import { RadioOption } from 'components/common/Radio/types';

import { FormValues } from './types';

function toOptionsArray<T extends object, O extends keyof T>(
  enumerable: T,
  unknown: O
): Array<SelectOption> {
  return Object.values(enumerable).reduce<SelectOption[]>((acc, cur) => {
    if (cur !== unknown) {
      const option: SelectOption = { label: cur, value: cur };
      acc.push(option);
    }

    return acc;
  }, []);
}

export const resourceTypes: Array<SelectOption> = toOptionsArray(
  KafkaAclResourceType,
  KafkaAclResourceType.UNKNOWN
);

export const operations = toOptionsArray(
  KafkaAclOperationEnum,
  KafkaAclOperationEnum.UNKNOWN
);

export const permissions: RadioOption[] = [
  {
    value: KafkaAclPermissionEnum.ALLOW,
    activeColor: { background: '#33CC66', color: 'white' },
  },
  {
    value: KafkaAclPermissionEnum.DENY,
    activeColor: { background: 'red', color: 'white' },
  },
];

export const defaultValues: Partial<FormValues> = {
  resourceType: resourceTypes[0].value as KafkaAclResourceType,
  operation: operations[0].value as KafkaAclOperationEnum,
};
