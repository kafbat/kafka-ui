import { Locator, Page } from '@playwright/test';

export class BaseLocators {
  // eslint-disable-next-line no-unused-vars
  constructor(private page: Page) {}

  get loadingSpinner(): Locator { return this.page.locator('div[role="progressbar"]'); }
  get submitBtn(): Locator { return this.page.locator('button[type="submit"]'); }
  get tableGrid(): Locator { return this.page.locator('table'); }
  get searchFld(): Locator { return this.page.locator('input[type="text"][id*=":r"]'); }
  get dotMenuBtn(): Locator { return this.page.locator('button[aria-label="Dropdown Toggle"]'); }
  get alertHeader(): Locator { return this.page.locator('div[role="alert"] div[role="heading"]'); }
  get alertMessage(): Locator { return this.page.locator('div[role="alert"] div[role="contentinfo"]'); }
  get confirmationMdl(): Locator { return this.page.locator('text=Confirm the action').locator('..'); }
  get confirmBtn(): Locator { return this.page.locator('button:has-text("Confirm")'); }
  get cancelBtn(): Locator { return this.page.locator('button:has-text("Cancel")'); }
  get backBtn(): Locator { return this.page.locator('button:has-text("Back")'); }
  get previousBtn(): Locator { return this.page.locator('button:has-text("Previous")'); }
  get nextBtn(): Locator { return this.page.locator('button:has-text("Next")'); }
  get ddlOptions(): Locator { return this.page.locator('li[value]'); }
  get gridItems(): Locator { return this.page.locator('tr[class]'); }

  tableElementNameLocator = (name: string):Locator => this.page.locator(`tbody a:has-text("${name}")`);
  pageTitleFromHeader = (title: string):Locator => this.page.locator(`h1:has-text("${title}")`);
  pagePathFromHeader = (title: string):Locator => this.page.locator(`a:has-text("${title}") >> .. >> h1`);
}