import { Option } from 'components/common/NewTable/Filter/variants/MultiSelect/types';

function toOption(value: string): Option {
  return { label: value, value };
}

export default toOption;
