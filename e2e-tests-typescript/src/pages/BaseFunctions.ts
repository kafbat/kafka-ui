import { Locator, Page, expect } from '@playwright/test';
import { Keyboard } from 'playwright-core';
import { BaseLocators } from './BaseLocators'; 

export enum AlertHeader {
  SUCCESS = 'Success',
  VALIDATION_ERROR = 'Validation Error',
  BAD_REQUEST = '400 Bad Request',
}

export class BaseHelper {
  constructor(private page: Page, private locators = new BaseLocators(page)) {}

  async waitUntilSpinnerDisappear(timeout = 60000) {
    if (await this.locators.loadingSpinner.isVisible({ timeout: 2000 }).catch(() => false)) {
      await this.locators.loadingSpinner.waitFor({ state: 'hidden', timeout });
    }
  }

  async clickClearSearchFieldButton() {
    const clearBtn = this.locators.searchFld.locator('.. >> span[role="button"] >> *');
    await clearBtn.click();
    await this.waitUntilSpinnerDisappear(1000);
  }

  async searchItem(tag: string) {
    await this.clickClearSearchFieldButton();
    await this.locators.searchFld.fill(tag);
    await this.locators.searchFld.press('Enter');
    await expect(this.locators.searchFld).toHaveValue(tag);
    await this.waitUntilSpinnerDisappear(1000);
  }

  getPageTitleFromHeader(title: string): Locator {
    return this.locators.pageTitleFromHeader(title);
  }

  getPagePathFromHeader(title: string): Locator {
    return this.locators.pagePathFromHeader(title);
  }

  async clickSubmitBtn() {
    await this.locators.submitBtn.click();
  }

  async clickNavigationBtn(type: 'next' | 'previous' | 'back') {
    const btn = {
      next: this.locators.nextBtn,
      previous: this.locators.previousBtn,
      back: this.locators.backBtn,
    }[type];

    await btn.click();
  }

  async setJsonInputValue(locator: Locator, json: string) {
    await locator.click();
    await locator.fill(json.replace(/  /g, ''));
    const keyboard: Keyboard = this.page.keyboard;
    await keyboard.down('Shift');
    await keyboard.press('PageDown');
    await keyboard.up('Shift');
    await keyboard.press('Delete');
  }

  getTableElement(name: string): Locator {
    return this.locators.tableElementNameLocator(name);
  }

  getDdlOptions(): Locator {
    return this.locators.ddlOptions;
  }

  async getAlertHeader(): Promise<string> {
    await expect(this.locators.alertHeader).toBeVisible();
    return (await this.locators.alertHeader.textContent()) ?? '';
  }

  async getAlertMessage(): Promise<string> {
    await expect(this.locators.alertMessage).toBeVisible();
    return (await this.locators.alertMessage.textContent()) ?? '';
  }

  async isAlertVisible(header: AlertHeader, message?: string): Promise<boolean> {
    const headerText = await this.getAlertHeader();
    const headerMatch = headerText === header;
    if (!message) return headerMatch;

    const messageText = await this.getAlertMessage();
    return headerMatch && messageText === message;
  }

  async clickConfirmButton() {
    await expect(this.locators.confirmBtn).toBeEnabled();
    await this.locators.confirmBtn.click();
    await expect(this.locators.confirmBtn).toBeHidden();
  }

  async clickCancelButton() {
    await expect(this.locators.cancelBtn).toBeEnabled();
    await this.locators.cancelBtn.click();
    await expect(this.locators.cancelBtn).toBeHidden();
  }

  async isConfirmationModalVisible(): Promise<boolean> {
    return await this.locators.confirmationMdl.isVisible().catch(() => false);
  }
}