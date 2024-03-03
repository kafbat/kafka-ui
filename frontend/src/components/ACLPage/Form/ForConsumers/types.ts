import { Option } from 'react-multi-select-component';
import { PrefixType } from 'components/ACLPage/Form/types';

export type FormValues = {
  principal: string;
  host: string;
  topics?: Option[];
  topicsPrefix?: string;
  consumerGroups?: Option[];
  consumerGroupsPrefix?: string;
};
