/* eslint-disable no-unused-vars */
import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";
import { expectVisibility, ensureCheckboxState } from "../services/uiHelper";
import { PlaywrightWorld } from "../support/PlaywrightWorld";

setDefaultTimeout(60 * 1000 * 2);

Given('Connectors with name: {string} visible is: {string}', async function(this: PlaywrightWorld, name: string, visible: string) {
    await expectVisibility(this.locators.connectors.rowData(name), visible);
});

When('Connectors searchfield input is: {string}', async function(this: PlaywrightWorld, value: string) {
    let inputField = this.locators.connectors.searchBox
    await inputField.isVisible()
    await inputField.fill(value);
    await this.locators.connectors.searchButton.click()

});

Given('Connectors satus is: {string}, type is: {string}', async function(this: PlaywrightWorld, status: string, type: string) {
    await this.locators.connectors.cellData(status).isVisible();
    await this.locators.connectors.cellData(type).isVisible();
});

Given('Connectors row menu menu item {string} is clicked', async function(this: PlaywrightWorld, menuItem: string) {
    await this.locators.connectors.rowMenu.click();
    await this.locators.connectors.rowMenuItem(menuItem).click();
});

When('Connectors connector named: {string} clicked', async function(this: PlaywrightWorld, name: string) {
    await this.locators.connectors.rowData(name).click()
});

Then('Connectors connector page with label: {string} open', async function(this: PlaywrightWorld, label: string) {
    await this.locators.connectors.localMenuLabel(label).isVisible()
});

Given('Connectors connector page status is: {string}', async function(this: PlaywrightWorld, status: string) {
    await this.locators.connectors.internalMenuCell(status).isVisible();
});

When('Connectors connector menu item {string} clicked', async function(this: PlaywrightWorld, menuItem: string) {
    await this.locators.connectors.internalMenuButton.click();
    await this.locators.connectors.internalMenuButtonItem(menuItem).click();
});

Given('Connectors connector page state is: {string}', async function(this: PlaywrightWorld, state: string) {
    await this.locators.connectors.internalMenuState(state).isVisible();
});