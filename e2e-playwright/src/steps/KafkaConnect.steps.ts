
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expectVisibility } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";
import { expect } from "@playwright/test";

setDefaultTimeout(60 * 1000 * 2);

Given('KafkaConnect with name: {string} visible is: {string}', async function(this: PlaywrightWorld, name: string, visible: string) {
    await expectVisibility(this.locators.connectors.rowData(name), visible);
});

When('KafkaConnect cell element {string} is clicked', async function(this: PlaywrightWorld, value: string) {
    await this.locators.connectors.cellData(value).click();
});

When('KafkaConnect searchfield input is: {string}', async function(this: PlaywrightWorld, value: string) {
    let inputField = this.locators.connectors.searchBox
    await inputField.isVisible()
    await inputField.fill(value);
});

Given('KafkaConnect satus is: {string}, type is: {string}', async function(this: PlaywrightWorld, status: string, type: string) {
    await this.locators.connectors.cellData(status).isVisible();
    await this.locators.connectors.cellData(type).isVisible();
});

Given('KafkaConnect row menu menu item {string} is clicked', async function(this: PlaywrightWorld, menuItem: string) {
        const { page } = this;
    const rowMenu = this.locators.connectors.rowMenu;
    const rowMenuItem = this.locators.connectors.rowMenuItem(menuItem);

    async function attemptClick() {
      await expect(rowMenu).toBeVisible({ timeout: 5000 });
      await expect(rowMenu).toBeEnabled({ timeout: 5000 });
      await rowMenu.scrollIntoViewIfNeeded();

      await rowMenu.click();

      await expect(rowMenuItem).toBeVisible({ timeout: 5000 });
      await rowMenuItem.click();
    }

    try {
      await attemptClick();
    } catch (firstErr) {
      await page.reload({ waitUntil: 'domcontentloaded' });
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});

      await attemptClick();
    }
});

When('KafkaConnect connector named: {string} clicked', async function(this: PlaywrightWorld, name: string) {
    await this.locators.connectors.rowData(name).click()
});

Then('KafkaConnect connector page with label: {string} open', async function(this: PlaywrightWorld, label: string) {
    await this.locators.connectors.localMenuLabel(label).isVisible()
});

Given('KafkaConnect connector page status is: {string}', async function(this: PlaywrightWorld, status: string) {
    await this.locators.connectors.internalMenuCell(status).isVisible();
});

When('KafkaConnect connector menu item {string} clicked', async function(this: PlaywrightWorld, menuItem: string) {
    const { page } = this;
    const menuButton = this.locators.connectors.internalMenuButton;
    const menuItemLocator = this.locators.connectors.internalMenuButtonItem(menuItem);

    async function attemptClick() {
      await expect(menuButton).toBeVisible({ timeout: 5000 });
      await expect(menuButton).toBeEnabled({ timeout: 5000 });

      await menuButton.click();

      await expect(menuItemLocator).toBeVisible({ timeout: 5000 });
      await menuItemLocator.click();
    }

    try {
      await attemptClick();
    } catch (firstErr) {
      await page.reload({ waitUntil: 'domcontentloaded' });

      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});

      await attemptClick();
    }
});

Given('KafkaConnect connector page state is: {string}', async function(this: PlaywrightWorld, state: string) {
    await this.locators.connectors.internalMenuState(state).isVisible();
});
