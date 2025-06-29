import { Option } from 'components/common/NewTable/ColumnFilter/variants/MultiSelect/types';

function customValueRenderer(selected: Option[]) {
  return selected.length ? selected.length : ' ';
}

export default customValueRenderer;
