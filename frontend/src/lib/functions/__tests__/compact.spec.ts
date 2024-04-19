import compact from 'lib/functions/compact';

describe('compact', () => {
  it('should remove falsey values from the array', () => {
    const input = [0, 1, false, 2, '', 3, null, undefined, 4, NaN, 5];
    const expected = [1, 2, 3, 4, 5];
    expect(compact(input)).toEqual(expected);
  });

  it('should return an empty array if all values are falsey', () => {
    const input = [0, false, '', null, undefined, NaN];
    const expected: number[] = [];
    expect(compact(input)).toEqual(expected);
  });

  it('should return a new array with only truthy values preserved', () => {
    const input = [1, 'hello', true, [], { a: 1 }, 42];
    const expected = [1, 'hello', true, [], { a: 1 }, 42];
    expect(compact(input)).toEqual(expected);
  });

  it('should preserve non-falsey values in their original order', () => {
    const input = [1, null, 2, undefined, 3, false, 4];
    const expected = [1, 2, 3, 4];
    expect(compact(input)).toEqual(expected);
  });

  it('should not modify the original array', () => {
    const input = [0, 1, 2, false, '', null, undefined, NaN];
    const inputCopy = [...input];
    compact(input);
    expect(input).toEqual(inputCopy);
  });
});
