import { Option } from '../types';

function customValueRenderer(selected: Option[]) {
  return selected.length ? selected.length : ' ';
}

export default customValueRenderer;
