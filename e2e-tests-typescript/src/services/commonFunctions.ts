import { v4 as uuidv4 } from 'uuid';

export const generateName = (prefix: string): string => {
  return `${prefix}-${uuidv4().slice(0, 8)}`;
};