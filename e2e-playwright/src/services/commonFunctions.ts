import { v4 as uuidv4 } from 'uuid';
import { expect, Locator, Page } from "@playwright/test";

export const generateName = (prefix: string): string => {
  return `${prefix}${uuidv4().slice(0, 8)}`;
};

export async function Delete(page: Page, times: number = 5): Promise<void> {
  for (let i = 0; i < times; i++) {
    await page.keyboard.press('Delete');
  }
}

export async function clearWithSelectAll(page: Page): Promise<void> {
  const selectAll = process.platform === 'darwin' ? 'Meta+A' : 'Control+A';
  await page.keyboard.press(selectAll);
  await page.keyboard.press('Backspace');
}

export async function clickMenuThenItem(
  page: Page,
  expectable : Locator,
  result: Locator
): Promise<void> {
  const VISIBLE_TIMEOUT = 5000;
  const ENABLED_TIMEOUT = 5000;
  const NETWORK_IDLE_TIMEOUT = 10000;

  async function attemptClick() {
    await expect(expectable).toBeVisible({ timeout: VISIBLE_TIMEOUT });
    await expect(expectable).toBeEnabled({ timeout: ENABLED_TIMEOUT });
    await expectable.scrollIntoViewIfNeeded();
    await expectable.click();

    await expect(result).toBeVisible({ timeout: VISIBLE_TIMEOUT });
    await result.click();
  }

  try {
    await attemptClick();
  } catch {
    await page.reload({ waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: NETWORK_IDLE_TIMEOUT }).catch(() => {});
    await attemptClick();
  }
}
