import { Locator, Page } from '@playwright/test';

export class BaseLocators {
  constructor(private page: Page) {}

  loadingSpinner:Locator = this.page.locator('div[role="progressbar"]');
  submitBtn:Locator = this.page.locator('button[type="submit"]');
  tableGrid:Locator = this.page.locator('table');
  searchFld:Locator = this.page.locator('input[type="text"][id*=":r"]');
  dotMenuBtn:Locator = this.page.locator('button[aria-label="Dropdown Toggle"]');
  alertHeader:Locator = this.page.locator('div[role="alert"] div[role="heading"]');
  alertMessage:Locator = this.page.locator('div[role="alert"] div[role="contentinfo"]');
  confirmationMdl:Locator = this.page.locator('text=Confirm the action').locator('..');
  confirmBtn:Locator = this.page.locator('button:has-text("Confirm")');
  cancelBtn:Locator = this.page.locator('button:has-text("Cancel")');
  backBtn:Locator = this.page.locator('button:has-text("Back")');
  previousBtn:Locator = this.page.locator('button:has-text("Previous")');
  nextBtn:Locator = this.page.locator('button:has-text("Next")');
  ddlOptions:Locator = this.page.locator('li[value]');
  gridItems:Locator = this.page.locator('tr[class]');

  tableElementNameLocator = (name: string):Locator => this.page.locator(`tbody a:has-text("${name}")`);
  pageTitleFromHeader = (title: string):Locator => this.page.locator(`h1:has-text("${title}")`);
  pagePathFromHeader = (title: string):Locator => this.page.locator(`a:has-text("${title}") >> .. >> h1`);
}