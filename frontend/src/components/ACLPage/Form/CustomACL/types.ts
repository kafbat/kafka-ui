import {
  KafkaAclOperationEnum,
  KafkaAclPermissionEnum,
  KafkaAclResourceType,
} from 'generated-sources';
import { MatchType } from 'components/ACLPage/Form/types';

export interface FormValues {
  resourceType: KafkaAclResourceType;
  resourceName: string;
  namePatternType: MatchType;
  principal: string;
  host: string;
  operation: KafkaAclOperationEnum;
  permission: KafkaAclPermissionEnum;
}
