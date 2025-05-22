import { Page, Locator } from "@playwright/test";

export default class TopicCreateLocators {
    private readonly page: Page;

    constructor(page: Page) {
      this.page = page;
    }

    topicsCreateHeading = (): Locator => this.page.getByText('TopicsCreate');
    topicsCreateTopicName = ():Locator => this.page.getByRole('textbox', { name: 'Topic Name *' });
    topicsCreateNumberOfPartitions  = ():Locator => this.page.getByRole('spinbutton', { name: 'Number of Partitions *' });
    topicsCreateCleanupPolicy = (): Locator => this.page.getByRole('listbox', { name: 'Cleanup policy' });
    topicsCreateCleanupPolicySelect = (value : string): Locator => this.page.getByRole('list').getByRole('option', { name: value, exact: true });
    topicsCreateMinInSyncReplicas = (): Locator => this.page.getByRole('spinbutton', { name: 'Min In Sync Replicas' });
    topicsCreateReplicationFactor = (): Locator => this.page.getByRole('spinbutton', { name: 'Replication Factor' });
    topicsCreateTimeToRetainData = (): Locator => this.page.getByRole('spinbutton', { name: 'Time to retain data (in ms)' });
    topicsCreate12Hours = (): Locator => this.page.getByRole('button', { name: 'hours' });
    topicsCreate1Day = (): Locator => this.page.getByRole('button', { name: '1 day' });
    topicsCreate2Day = (): Locator => this.page.getByRole('button', { name: '2 days' });
    topicsCreate7Day = (): Locator => this.page.getByRole('button', { name: '7 days' });
    topicsCreate4Weeks = (): Locator => this.page.getByRole('button', { name: 'weeks' });
    topicsCreateMaxPartitionSize = (): Locator => this.page.getByRole('listbox', { name: 'Max partition size in GB' });
    topicsCreateMaxPartitionSizeSelect = (value: string): Locator => this.page.getByRole('option', { name: value });
    topicsCreateMaxMessageSize = (): Locator => this.page.getByRole('spinbutton', { name: 'Maximum message size in bytes' });
    topicsCreateAddCustomParameter = (): Locator => this.page.getByRole('button', { name: 'Add Custom Parameter' });
    topicsCreateCancel = (): Locator => this.page.getByRole('button', { name: 'Cancel' });
    topicCreateCreateTopicButton = (): Locator => this.page.getByRole('button', { name: 'Create topic' });
}