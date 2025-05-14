import { v4 as uuidv4 } from 'uuid';
import { Locator} from '@playwright/test';

export const generateName = (prefix: string): string => {
  return `${prefix}-${uuidv4().slice(0, 8)}`;
};
