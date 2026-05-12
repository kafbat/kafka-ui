import { Page, Locator } from "@playwright/test";

export default class TopicCreateLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

  get heading(): Locator { return this.page.getByText('TopicsCreate'); }
  get topicName(): Locator { return this.page.getByRole('textbox', { name: 'Topic Name *' }); }
  get numberOfPartitions(): Locator { return this.page.getByRole('spinbutton', { name: 'Number of Partitions *' }); }
  get cleanupPolicy(): Locator { return this.page.getByRole('listbox', { name: 'Cleanup policy' }); }
  get minInSyncReplicas(): Locator { return this.page.getByRole('spinbutton', { name: 'Min In Sync Replicas' }); }
  get replicationFactor(): Locator { return this.page.getByRole('spinbutton', { name: 'Replication Factor' }); }
  get timeToRetainData(): Locator { return this.page.getByRole('spinbutton', { name: 'Time to retain data (in ms)' }); }
  get button12Hours(): Locator { return this.page.getByRole('button', { name: '12 hours' }); }
  get button1Day(): Locator { return this.page.getByRole('button', { name: '1 day' }); }
  get button2Day(): Locator { return this.page.getByRole('button', { name: '2 days' }); }
  get button7Day(): Locator { return this.page.getByRole('button', { name: '7 days' }); }
  get button4Weeks(): Locator { return this.page.getByRole('button', { name: 'weeks' }); }
  get maxPartitionSize(): Locator { return this.page.getByRole('listbox', { name: 'Max partition size in GB' }); }
  get maxMessageSize(): Locator { return this.page.getByRole('spinbutton', { name: 'Maximum message size in bytes' }); }
  get addCustomParameter(): Locator { return this.page.getByRole('button', { name: 'Add Custom Parameter' }); }
  get cancel(): Locator { return this.page.getByRole('button', { name: 'Cancel' }); }
  get createTopicButton(): Locator { return this.page.getByRole('button', { name: 'Create topic' }); }

  maxPartitionSizeSelect(value: string): Locator { return this.page.getByRole('option', { name: value }); }
  cleanupPolicySelect(value: string): Locator { return this.page.getByRole('list').getByRole('option', { name: value, exact: true }); }
}
