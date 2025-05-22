import { IWorldOptions, World } from '@cucumber/cucumber';
import { Page } from "@playwright/test";
import { Logger } from "winston";
import { BrowserContext } from "@playwright/test";
import { createLogger } from "winston";
import { options } from "../helper/util/logger";
import { Locators } from '../pages/Locators';

export class PlaywrightCustomWorld extends World {

  public logger?: Logger;
  public browserContext?: BrowserContext;

  private context: Map<string, any> = new Map();
  private _page?: Page;
  private _locators?:Locators;
  private _scenarioName?: string;

  constructor(options: IWorldOptions) {
    super(options);
  }

  setValue(key: string, value: any) {
    this.context.set(key, value);
  }

  getValue<T>(key: string): T {
    const value = this.context.get(key);
    if (value === undefined) throw new Error(`Key '${key}' not found in context.`);
    return value as T;
  }

  async init(context: BrowserContext, scenarioName:string ) {
      const page = await context.newPage();

      this._page = page;
      this.logger = createLogger(options(scenarioName));
      this._scenarioName = scenarioName;
      this.browserContext = context;
  }

  get page() : Page {
    if (this._page) {
      return this._page!;
    }

    throw new Error("No page");
  }

  get locators() : Locators {
      return (this._locators ??= new Locators(this.page));
  }

  get scenarioName(): string {
    return this._scenarioName ?? "No Name";
  }

  clear() {
    this.context.clear();
  }
}
