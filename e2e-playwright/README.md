# 🧪 kafbat-e2e-playwright
End-to-End UI test automation using **Playwright**, **Cucumber.js**, and **TypeScript**.
---

## Prerequisites

- **Node.js** >= 18
- **pnpm** (bundled via Corepack: `corepack enable`)
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
        uses: actions/checkout@v4

      - name: Set up pnpm
        uses: pnpm/action-setup@v4
        with:
          version: 10.33.0

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 18
          cache: 'pnpm'
          cache-dependency-path: ./e2e-playwright/pnpm-lock.yaml

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Install Playwright browsers
        run: pnpm exec playwright install

      - name: 🚀 Run tests with ENV=prod
        run: ENV=prod HEAD=false BASEURL=http://localhost:8080 pnpm test
```
