import { type Option } from '../types';

function toOption(value: string): Option {
  return { label: value, value };
}

export default toOption;
