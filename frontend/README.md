# Kafbat UI
Web UI for managing Apache Kafka clusters

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)

## Table of contents
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Links](#links)

## Requirements
- [docker](https://www.docker.com/get-started) (required to run [Initialize application](#initialize-application))
- [nvm](https://github.com/nvm-sh/nvm) with installed [Node.js](https://nodejs.org/en/) of expected version (check `.nvmrc`)

## Getting started

Go to the React app folder
```sh
cd ./frontend
```

Install [pnpm](https://pnpm.io/installation)
```
npm install -g pnpm
```

Update pnpm
```
npm rm -g pnpm
```
Then reinstall it

or use
```
npm install -g pnpm@<version>
```

Install dependencies
```
pnpm install
```

Generate API clients from the OpenAPI document
```sh
pnpm gen:sources
```

## Start application
### Proxying API Requests in Development

Create or update the existing `.env.local` file with
```
VITE_DEV_PROXY= https://api.server # your API server
```

Run the application
```sh
pnpm dev
```

### Docker way

Must be run from the root directory.

Start Kafbat UI with your Kafka clusters:
```sh
docker-compose -f ./documentation/compose/kafbat-ui.yaml up
```

Make sure that none of the `.env*` files contain the `DEV_PROXY` variable

Run the application
```sh
pnpm dev
```
## Links

* [Vite](https://github.com/vitejs/vite)
