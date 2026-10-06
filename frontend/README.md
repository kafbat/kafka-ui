# Kafbat UI
Web UI for managing Apache Kafka clusters

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.kafbat%3Akafka-ui_frontend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=io.kafbat%3Akafka-ui_frontend)

## Table of contents
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Large message previews](#large-message-previews)
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
## Large message previews

The message table displays at most 512 characters of each key and value, including
tooltips. JSONPath previews are skipped for fields larger than 64 Ki characters.
Expanded fields larger than 64 Ki characters use unformatted plain-text previews;
**Show more** replaces the current preview with the next bounded chunk rather than
appending content indefinitely. Large headers have a bounded prefix preview.

**Download full content**, clipboard actions, record downloads, and JSON/CSV exports
use the original data, not the truncated preview. Exports are generated only when
requested. Opening a large record in the producer editor requires confirmation.
These are frontend rendering limits, not Kafka byte limits: polling, filters, and
pagination are unchanged. Full records still arrive through SSE and reside in
browser memory; arbitrarily large records or prolonged live consumption can still
exhaust memory.

## Links

* [Vite](https://github.com/vitejs/vite)
