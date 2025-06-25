import { Option } from 'components/common/NewTable/Filter/variants/MultiSelect/types';

function customValueRenderer(selected: Option[]) {
  return selected.length ? selected.length : ' ';
}

export default customValueRenderer;
