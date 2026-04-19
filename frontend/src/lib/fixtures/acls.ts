import {
  KafkaAcl,
  KafkaAclResourceType,
  KafkaAclNamePatternType,
  KafkaAclPermissionEnum,
  KafkaAclOperationEnum,
} from 'generated-sources';

export const aclPayload: KafkaAcl[] = [
  {
    principal: 'User 1',
    resourceName: 'Topic',
    resourceType: KafkaAclResourceType.TOPIC,
    host: '192.168.0.2',
    namePatternType: KafkaAclNamePatternType.LITERAL,
    permission: KafkaAclPermissionEnum.ALLOW,
    operation: KafkaAclOperationEnum.READ,
  },
  {
    principal: 'User 2',
    resourceName: 'Topic',
    resourceType: KafkaAclResourceType.TOPIC,
    host: '*',
    namePatternType: KafkaAclNamePatternType.PREFIXED,
    permission: KafkaAclPermissionEnum.ALLOW,
    operation: KafkaAclOperationEnum.READ,
  },
  {
    principal: 'User 3',
    resourceName: 'Topic',
    resourceType: KafkaAclResourceType.TOPIC,
    host: '192.168.0.1',
    namePatternType: KafkaAclNamePatternType.LITERAL,
    permission: KafkaAclPermissionEnum.DENY,
    operation: KafkaAclOperationEnum.READ,
  },
];
