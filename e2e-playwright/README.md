# 🧪 kafbat-e2e-playwright
End-to-End UI test automation using **Playwright**, **Cucumber.js**, and **TypeScript**.
---

## Prerequisites

- **Node.js** >= 18  
- **pnpm**
- Install dependencies and Playwright browsers:

```bash
Local run:
Run Kafbat (docker compose -f ./documentation/compose/e2e-tests.yaml up -d)
pnpm install --frozen-lockfile
pnpm exec playwright install

🔹 Normal Test Run
pnpm test:stage

🔹 Debug Mode (with Playwright Inspector)
pnpm run debug

🔹 Rerun Failed Tests
pnpm run test:failed


GitHub Actions CI example
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  container-test-job:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repo
        uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@3235b876344d2a9aa001b8d1453c930bba69e610 # https://github.com/actions/setup-node/releases/tag/v3.9.1
        with:
          node-version: 18

      - name: Install NPM dependencies
        run: pnpm install --frozen-lockfile

      - name: Install Playwright browsers
        run: pnpm exec playwright install

      - name: 🚀 Run tests with ENV=prod
        run: ENV=prod HEAD=false BASEURL=http://localhost:8080 pnpm test
```
