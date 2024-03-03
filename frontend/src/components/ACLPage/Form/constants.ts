import { SelectOption } from 'components/common/Select/Select';
import { RadioOption } from 'components/common/Radio/types';

import { ACLType, PrefixType } from './types';

export const prefixOptions: RadioOption[] = [
  {
    value: PrefixType.EXACT,
  },
  { value: PrefixType.PREFIXED },
];

export const ACLTypeOptions: SelectOption[] = [
  { label: 'Custom ACL', value: ACLType.CUSTOM_ACL },
  { label: 'For Consumers', value: ACLType.FOR_CONSUMERS },
  { label: 'For Producers', value: ACLType.FOR_PRODUCERS },
  { label: 'For Kafka Stream Apps', value: ACLType.FOR_KAFKA_STREAM_APPS },
];
