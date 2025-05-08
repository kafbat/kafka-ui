import { Given, When, Then, setDefaultTimeout } from "@cucumber/cucumber";

import { expect } from "@playwright/test";
import { fixture } from "../hooks/pageFixture";

setDefaultTimeout(60 * 1000 * 2)

Given('User navigates to {string}', async function (pageName: string) {
    console.log(pageName);
    await fixture.page.goto(process.env.BASEURL!);
    await fixture.page.getByRole('link', { name: pageName }).click();
});
  
Then('Page {string} opened', async function (name: string) {
  await fixture.page.getByRole('link', { name: name }).waitFor({ state: 'visible' });
});

Then('Page Connectors opened', async function () {
  await fixture.page.getByText('ConnectorsCreate Connector').waitFor({ state: 'visible' });
});