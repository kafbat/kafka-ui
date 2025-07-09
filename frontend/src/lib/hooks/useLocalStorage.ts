import { LOCAL_STORAGE_KEY_PREFIX } from 'lib/constants';
import {
  useState,
  useEffect,
  Dispatch,
  SetStateAction,
  useCallback,
} from 'react';

const subscribers = new Map<string, Set<() => void>>();

const notifySubscribers = (key: string) => {
  const keySubscribers = subscribers.get(key);
  if (keySubscribers) {
    keySubscribers.forEach((callback) => callback());
  }
};

const subscribe = (key: string, callback: () => void) => {
  if (!subscribers.has(key)) {
    subscribers.set(key, new Set());
  }
  subscribers.get(key)!.add(callback);

  return () => {
    const keySubscribers = subscribers.get(key);
    if (keySubscribers) {
      keySubscribers.delete(callback);
      if (keySubscribers.size === 0) {
        subscribers.delete(key);
      }
    }
  };
};

const getStorageValue = <T>(key: string, defaultValue: T): T => {
  try {
    const saved = localStorage.getItem(key);
    if (saved !== null) {
      return JSON.parse(saved);
    }
  } catch (error) {
    console.warn(`Failed to read localStorage key "${key}":`, error);
  }
  return defaultValue;
};

const setStorageValue = <T>(key: string, value: T): void => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    notifySubscribers(key);
  } catch (error) {
    console.warn(`Failed to write localStorage key "${key}":`, error);
  }
};

export const useLocalStorage = <T>(
  featureKey: string,
  defaultValue: T
): [T, Dispatch<SetStateAction<T>>] => {
  const key = `${LOCAL_STORAGE_KEY_PREFIX}-${featureKey}`;

  const [value, setValue] = useState<T>(() =>
    getStorageValue(key, defaultValue)
  );

  const setStoredValue = useCallback<Dispatch<SetStateAction<T>>>(
    (newValue) => {
      setValue((prevValue) => {
        const valueToStore =
          typeof newValue === 'function'
            ? (newValue as (prevState: T) => T)(prevValue)
            : newValue;

        setStorageValue(key, valueToStore);
        return valueToStore;
      });
    },
    [key]
  );

  useEffect(() => {
    const handleStorageChange = () => {
      const newValue = getStorageValue(key, defaultValue);
      setValue(newValue);
    };

    const unsubscribe = subscribe(key, handleStorageChange);

    const handleStorageEvent = (e: StorageEvent) => {
      if (e.key === key) {
        handleStorageChange();
      }
    };

    window.addEventListener('storage', handleStorageEvent);

    return () => {
      unsubscribe();
      window.removeEventListener('storage', handleStorageEvent);
    };
  }, [key, defaultValue]);

  return [value, setStoredValue];
};
