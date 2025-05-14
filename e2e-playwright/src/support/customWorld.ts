import { IWorldOptions, setWorldConstructor, World } from '@cucumber/cucumber';

export class CustomWorld extends World {
  private context: Map<string, any> = new Map();

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

  clear() {
    this.context.clear();
  }
}

setWorldConstructor(CustomWorld);