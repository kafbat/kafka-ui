import { v4 as uuidv4 } from 'uuid';
import { Page } from "@playwright/test";

export const generateName = (prefix: string): string => {
  return `${prefix}${uuidv4().slice(0, 8)}`;
};

export async function Delete(page: Page, times: number = 5): Promise<void> {
  for (let i = 0; i < times; i++) {
    await page.keyboard.press('Delete');
  }
}

// export const generateName = (prefix: string): string => {
//   return `${prefix}-${uuidv4().slice(0, 8)}`;
// };
