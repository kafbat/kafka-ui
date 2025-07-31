# 🧪 kafbat-e2e-playwright
End-to-End UI test automation using **Playwright**, **Cucumber.js**, and **TypeScript**.
---

## Prerequisites

- **Node.js** >= 18  
- **npm** or **yarn**
- Install dependencies and Playwright browsers:

```bash
Local run:
Run kafbat (docker compose -f ./documentation/compose/e2e-tests.yaml up -d)
npm install
npx playwright install

🔹 Normal Test Run
npm test:stage

🔹 Debug Mode (with Playwright Inspector)
npm run debug

🔹 Rerun Failed Tests
npm run test:failed


Gihub action CI example
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
        uses: actions/setup-node@v3
        with:
          node-version: 18

      - name: Install NPM dependencies
        run: npm install

      - name: Install Playwright browsers
        run: npx playwright install

      - name: 🚀 Run tests with ENV=prod
        run: ENV=prod HEAD=false BASEURL=http://localhost:8080 npm run test
        