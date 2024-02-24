import { keyBy } from 'lib/functions/keyBy';

describe('keyBy', () => {
  it('returns grouped object', () => {
    const original = [
      { id: 100, host: 'b-1.test.kafka.amazonaws.com', port: 9092 },
      { id: 200, host: 'b-2.test.kafka.amazonaws.com', port: 9092 },
    ];
    const expected = {
      100: { id: 100, host: 'b-1.test.kafka.amazonaws.com', port: 9092 },
      200: { id: 200, host: 'b-2.test.kafka.amazonaws.com', port: 9092 },
    };
    const result = keyBy(original, 'id');

    expect(result).toEqual(expected);
  });
});
