import { Locator } from '@playwright/test';
import expect from "../helper/util/expect";

 export const expectVisibility = async (locator: Locator, visibleString: string): Promise<void> => {
  if (visibleString === "true") {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toHaveCount(0);
  }
};

export const ensureCheckboxState = async (checkbox: Locator, expectedState: string) => {
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
