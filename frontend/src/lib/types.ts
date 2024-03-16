export type ObjectValues<T extends Record<string, unknown>> = T[keyof T];

export type WithPartialKey<T, K extends keyof T> = Omit<T, K> &
  Partial<Record<K, Partial<T[K]>>>;
