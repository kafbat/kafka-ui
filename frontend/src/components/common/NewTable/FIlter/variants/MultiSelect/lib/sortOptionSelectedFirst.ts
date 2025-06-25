import { Option } from 'components/common/NewTable/Filter/variants/MultiSelect/types';

function sortOptionSelectedFirst(selected: Option[], all: Option[]): Option[] {
  return [
    ...selected,
    ...all.filter(
      (option) =>
        !selected.some((selectdOption) => selectdOption.value === option.value)
    ),
  ];
}

export default sortOptionSelectedFirst;
