# 🧪 kafbat-e2e-playwright
End-to-End UI test automation using **Playwright**, **Cucumber.js**, and **TypeScript**.
---

## Prerequisites

- **Node.js** >= 18  
- **npm** or **yarn**
- Install dependencies and Playwright browsers:

```bash
npm install
npx playwright install

🚀 How to Run Head = true for development and debug

🔹 Normal Test Run
npm test:stage

🔹 Debug Mode (with Playwright Inspector)
npm run debug

🔹 Rerun Failed Tests
npm run test:failed


🚀 How to Run Docker image
 docker build -t kafbat-e2e .
 docker run --rm kafbat-e2e