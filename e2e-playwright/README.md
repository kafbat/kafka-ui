# 🧪 kafbat-e2e-playwright
End-to-End UI test automation using **Playwright**, **Cucumber.js**, and **TypeScript**.
---

## Prerequisites

- **Node.js** >= 20 (CI uses 22)
- **pnpm**: `corepack enable` picks the version from `package.json`
- Kafbat UI running on `http://localhost:8080`, e.g. `docker compose -f documentation/compose/e2e-tests.yaml up -d` from the repository root

## Running

All commands run from `e2e-playwright/`:

```bash
pnpm install --frozen-lockfile
pnpm exec playwright install

# Normal test run
pnpm test:stage

# Debug mode (with Playwright Inspector)
pnpm run debug

# Rerun failed tests
pnpm run test:failed
```

CI runs the same steps in [`e2e-playwright-run.yml`](../.github/workflows/e2e-playwright-run.yml).
