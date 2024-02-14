type AvailableKeys<A extends object> = keyof {
  [P in keyof A as A[P] extends PropertyKey ? P : never]: unknown;
};

export function keyBy<A extends object, K extends AvailableKeys<A>>(
  collection: A[] | undefined | null,
  property: K
) {
  if (collection === undefined || collection === null) {
    return {} as Record<PropertyKey, A>;
  }

  return collection.reduce<Record<PropertyKey, A>>((acc, cur) => {
    const key = cur[property] as unknown as PropertyKey;

    // eslint-disable-next-line no-param-reassign
    acc[key] = cur;

    return acc;
  }, {});
}
