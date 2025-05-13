import { expect, Locator } from '@playwright/test';

export const expectVisibility = async (locator: Locator, visibleString: string): Promise<void> => {
  const shouldBeVisible = visibleString === "true";

  if (shouldBeVisible) {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toHaveCount(0);
  }
};