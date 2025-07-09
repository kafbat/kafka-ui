### E2E UI automation for Kafbat UI

This repository is for E2E UI automation.

### Table of Contents

- [Prerequisites](#prerequisites)
- [How to install](#how-to-install)
- [How to run checks](#how-to-run-checks)
- [Reporting](#reporting)
- [Test Data](#test-data)
- [Actions](#actions)
- [Checks](#checks)
- [Parallelization](#parallelization)
- [How to develop](#how-to-develop)

### Prerequisites

- Docker & Docker-compose
- Java (install arch64 jdk if you have M1/arm chip)

### How to install

```
git clone https://github.com/kafbat/kafka-ui.git
cd  e2e-tests
docker pull selenoid/vnc_chrome:117.0 
```

### How to run checks

1. Run `kafbat-ui`:

```
cd kafbat-ui
docker-compose -f e2e-tests/selenoid/selenoid-ci.yaml up -d
docker-compose -f documentation/compose/e2e-tests.yaml up -d
```

2. To run test suite select its name (options: `regression`, `sanity`, `smoke`) and put it instead %s into command below

```
./gradlew :e2e-tests:test -Prun-e2e=true -Psuite_name=%s
```

### Reporting

Screenshots are stored in `target/selenide-results/reports` folder.

Reports are stored in `target/allure-results` folder.
If you have installed allure commandline [here](https://www.npmjs.com/package/allure-commandline), you can see allure report with command:

```
allure serve
```

### Test Data

> ⚠️ todo

### Actions

> ⚠️ todo

### Checks

> ⚠️ todo

### Parallelization

> ⚠️ todo

### How to develop

> ⚠️ todo
