type PropertyExtractorByKey<A, K extends keyof A> = {
  [P in A[K] as A[K] extends PropertyKey ? A[K] : never]: A;
};

export function keyBy<
  A extends object,
  K extends keyof {
    [P in keyof A as A[P] extends PropertyKey ? P : never]: unknown;
  }
>(collection: A[] | undefined | null, property: K) {
  if (collection === undefined || collection === null) {
    return {} as PropertyExtractorByKey<A, K>;
  }

  return collection.reduce((acc, cur) => {
    return { ...acc, [cur[property] as unknown as PropertyKey]: cur };
  }, {} as PropertyExtractorByKey<A, K>);
}
