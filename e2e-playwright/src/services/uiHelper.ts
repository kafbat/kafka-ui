import { Locator } from '@playwright/test';
import expect from "../helper/util/expect";
import { Page } from "@playwright/test";

 export const expectVisibility = async(locator: Locator, visibleString: string): Promise<void> => {
  if (visibleString === "true") {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toHaveCount(0);
  }
};

export const expectEnabled = async(locator: Locator, enabledString: string): Promise<void> => {
  const shouldBeEnabled = enabledString === "true";
  if (shouldBeEnabled) {
    await expect(locator).toBeEnabled();
  } else {
    await expect(locator).toBeDisabled();
  }
};

export const ensureCheckboxState = async(checkbox: Locator, expectedState: string) => {
  const desiredState = expectedState === 'true';
  const isChecked = await checkbox.isChecked();

  if (isChecked !== desiredState) {
    if (desiredState) {
      await checkbox.check();
    } else {
      await checkbox.uncheck();
    }
  }
};

export const expectVisuallyActive = async(
  locator: Locator,
  expected: string
): Promise<void> => {
  const shouldBeClickable = expected === 'true';

  const style = await locator.evaluate((el) => {
    const computed = window.getComputedStyle(el);
    return {
      pointerEvents: computed.pointerEvents,
      visibility: computed.visibility,
      display: computed.display,
      cursor: computed.cursor,
    };
  });

  const isClickable =
    style.pointerEvents !== 'none' &&
    style.visibility !== 'hidden' &&
    style.display !== 'none' &&
    style.cursor !== 'not-allowed';

  expect(isClickable).toBe(shouldBeClickable);
};

export async function refreshPageAfterDelay(page: Page, delayMs: number = 35000): Promise<void> {
  await page.waitForTimeout(delayMs);
  await page.reload();
}