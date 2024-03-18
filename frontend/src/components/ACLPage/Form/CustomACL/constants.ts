import { SelectOption } from 'components/common/Select/Select';
import {
  KafkaAclOperationEnum,
  KafkaAclPermissionEnum,
  KafkaAclResourceType,
} from 'generated-sources';
import { RadioOption } from 'components/common/Radio/types';

import { FormValues } from './types';

function toOptionsArray<T extends string>(
  list: T[],
  unknown: T
): SelectOption<T>[] {
  return list.reduce<SelectOption<T>[]>((acc, cur) => {
    if (cur !== unknown) {
      acc.push({ label: cur, value: cur });
    }

    return acc;
  }, []);
}

export const resourceTypes = toOptionsArray(
  Object.values(KafkaAclResourceType),
  KafkaAclResourceType.UNKNOWN
);

export const operations = toOptionsArray(
  Object.values(KafkaAclOperationEnum),
  KafkaAclOperationEnum.UNKNOWN
);

export const permissions: RadioOption[] = [
  {
    value: KafkaAclPermissionEnum.ALLOW,
    itemType: 'green',
  },
  {
    value: KafkaAclPermissionEnum.DENY,
    itemType: 'red',
  },
];

export const defaultValues: Partial<FormValues> = {
  resourceType: resourceTypes[0].value as KafkaAclResourceType,
  operation: operations[0].value as KafkaAclOperationEnum,
};
