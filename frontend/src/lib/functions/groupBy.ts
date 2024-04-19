export default function groupBy<T extends object>(
  collections: T[],
  key: string
) {
  return collections.reduce<Record<string, [T, ...T[]]>>((acc, curr) => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    const groupByKey = key in curr ? curr[key] : null;

    if (typeof groupByKey !== 'string' && typeof groupByKey !== 'number') {
      return acc;
    }

    if (acc[groupByKey]) {
      acc[groupByKey].push(curr);
      return acc;
    }

    acc[groupByKey] = [curr];
    return acc;
  }, {});
}
