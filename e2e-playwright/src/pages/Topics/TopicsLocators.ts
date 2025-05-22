import { Page, Locator } from "@playwright/test";

export default class TopicsLocators{
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    topicsHeading = (): Locator => this.page.getByRole("heading", { name: "Topics" });
    topicSearchField = (): Locator => this.page.getByRole('textbox', { name: 'Search by Topic Name' });
    topicSearchFieldCleanText = (): Locator => this.page.getByRole('button').filter({ hasText: /^$/ }).locator('path');
    topicShowInternalTopics = (): Locator => this.page.locator('label').filter({ hasText: 'Show Internal Topics' }).locator('span');
    topicAddTopicButton  = (): Locator => this.page.getByRole('button', { name: 'Add a Topic' });
    topicSelectAllCheckbox = (): Locator => this.page.getByRole('row', { name: 'Topic Name Partitions Out of' }).getByRole('checkbox');
    topicDeleteSelectedTopicsButton  = (): Locator => this.page.getByRole('button', { name: 'Delete selected topics' });
    topicCopySelectedTopicButton  = (): Locator => this.page.getByRole('button', { name: 'Copy selected topic' });
    topicPurgeMessagesOfSelectedTopicsButton  = (): Locator => this.page.getByRole('button', { name: 'Purge messages of selected' });
    topicSelectAllCheckBox = (): Locator => this.page.getByRole('row', { name: 'Topic Name Partitions Out of' }).getByRole('checkbox');
    topicRowCheckBox = (message:string): Locator => this.page.getByRole('row', { name: message }).getByRole('checkbox');
    topicNameLink = (value:string) : Locator => this.page.getByRole('link', { name: value })
}