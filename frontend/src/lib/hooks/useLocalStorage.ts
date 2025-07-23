import { LOCAL_STORAGE_KEY_PREFIX } from 'lib/constants';
import { useState, useEffect, Dispatch, SetStateAction } from 'react';

export const useLocalStorage = <T>(
  featureKey: string,
  defaultValue: T
): [T, Dispatch<SetStateAction<T>>] => {
  const key = `${LOCAL_STORAGE_KEY_PREFIX}-${featureKey}`;
  const [value, setValue] = useState<T>(() => {
    const saved = localStorage.getItem(key);

    if (saved !== null) {
      return JSON.parse(saved);
    }
    return defaultValue;
  });

  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  return [value, setValue];
};
