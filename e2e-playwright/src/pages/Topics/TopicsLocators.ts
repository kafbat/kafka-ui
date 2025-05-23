import { Page, Locator } from "@playwright/test";

export default class TopicsLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

  get heading(): Locator { return this.page.getByRole("heading", { name: "Topics" }); }
  get searchField(): Locator { return this.page.getByRole('textbox', { name: 'Search by Topic Name' }); }
  get searchFieldCleanText(): Locator { return this.page.getByRole('button').filter({ hasText: /^$/ }).locator('path'); }
  get showInternalTopics(): Locator { return this.page.locator('label').filter({ hasText: 'Show Internal Topics' }).locator('span'); }
  get addTopicButton(): Locator { return this.page.getByRole('button', { name: 'Add a Topic' }); }
  get selectAllCheckbox(): Locator { return this.page.getByRole('row', { name: 'Topic Name Partitions Out of' }).getByRole('checkbox'); }
  get deleteSelectedTopicsButton(): Locator { return this.page.getByRole('button', { name: 'Delete selected topics' }); }
  get copySelectedTopicButton(): Locator { return this.page.getByRole('button', { name: 'Copy selected topic' }); }
  get purgeMessagesOfSelectedTopicsButton(): Locator { return this.page.getByRole('button', { name: 'Purge messages of selected' }); }
  get selectAllCheckBox(): Locator { return this.page.getByRole('row', { name: 'Topic Name Partitions Out of' }).getByRole('checkbox'); }
  rowCheckBox(message: string): Locator { return this.page.getByRole('row', { name: message }).getByRole('checkbox'); }
  nameLink(value: string): Locator { return this.page.getByRole('link', { name: value }); }
}