import groupBy from 'lib/functions/groupBy';

describe('groupBy', () => {
  it('should group objects in the array by the specified key', () => {
    const input = [
      { id: 1, name: 'John' },
      { id: 2, name: 'Jane' },
      { id: 3, name: 'Doe' },
      { id: 4, name: 'John' },
    ];

    const result = groupBy(input, 'name');

    expect(result).toEqual({
      John: [
        { id: 1, name: 'John' },
        { id: 4, name: 'John' },
      ],
      Jane: [{ id: 2, name: 'Jane' }],
      Doe: [{ id: 3, name: 'Doe' }],
    });
  });

  it('should return an empty object when the input array is empty', () => {
    const result = groupBy([], 'name');

    expect(result).toEqual({});
  });

  it('should handle objects with undefined values for the specified key', () => {
    const input = [
      { id: 1, name: 'John' },
      { id: 2, name: undefined },
      { id: 3, name: 'Doe' },
      { id: 4 },
    ];

    const result = groupBy(input, 'name');

    expect(result).toEqual({
      John: [{ id: 1, name: 'John' }],
      Doe: [{ id: 3, name: 'Doe' }],
    });
  });
});
