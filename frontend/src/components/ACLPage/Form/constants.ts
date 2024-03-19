import { SelectOption } from 'components/common/Select/Select';
import { RadioOption } from 'components/common/Radio/types';

import { ACLType, MatchType } from './types';

export const matchTypeOptions: RadioOption[] = [
  {
    value: MatchType.EXACT,
  },
  { value: MatchType.PREFIXED },
];

export const ACLTypeOptions: SelectOption<ACLType>[] = [
  { label: 'Custom ACL', value: ACLType.CUSTOM_ACL },
  { label: 'For Consumers', value: ACLType.FOR_CONSUMERS },
  { label: 'For Producers', value: ACLType.FOR_PRODUCERS },
  { label: 'For Kafka Stream Apps', value: ACLType.FOR_KAFKA_STREAM_APPS },
];
