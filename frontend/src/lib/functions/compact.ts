/**
 * @description
 * Creates an array with all falsey values removed. The values false, null, 0, "", undefined, and NaN are
 * falsey.
 *
 * @param array The array to compact.
 * @return Returns the new array of filtered values.
 */
export default function compact<T>(
  array: Array<T | null | undefined | false | '' | 0>
): T[] {
  return array.filter(Boolean) as T[];
}
