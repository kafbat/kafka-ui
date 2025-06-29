import { Option } from 'components/common/NewTable/ColumnFilter/variants/MultiSelect/types';

function toOption(value: string): Option {
  return { label: value, value };
}

export default toOption;
