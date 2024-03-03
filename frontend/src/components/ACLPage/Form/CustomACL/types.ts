import {
  KafkaAclOperationEnum,
  KafkaAclPermissionEnum,
  KafkaAclResourceType,
} from 'generated-sources';
import { PrefixType } from 'components/ACLPage/Form/types';

export type FormValues = {
  resourceType: KafkaAclResourceType;
  resourceName: string;
  namePatternType: PrefixType;
  principal: string;
  host: string;
  operation: KafkaAclOperationEnum;
  permission: KafkaAclPermissionEnum;
};
