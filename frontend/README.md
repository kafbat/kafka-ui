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

The message table displays at most 512 UTF-16 code units of each key and value,
including tooltips. JSONPath previews are skipped for fields larger than 64 Ki
UTF-16 code units. Expanded fields above that limit use plain-text previews;
**Show next chunk** replaces the current preview with the next chunk, and
**Previous chunk** returns to the prior chunk. The warning shows the current
chunk number and preview range. Oversized records use the themed
**Preview truncated** warning, with navigation and **Download full message**
actions grouped together. Key/Value/Headers tabs and record metadata remain
available. Large headers have a bounded prefix preview with a full download,
rather than chunk navigation.

**Download full message** saves the full message as JSON, regardless of the selected field,
in a file named `<topic-name>-pN-oM.json`, using the actual topic name, partition,
and offset. The file includes the value, key, headers, partition, offset, and
timestamp. **Download value**, **Download key**, or **Download headers** saves
only the complete selected field, with `-value`, `-key`, or `-headers` appended
to `<topic-name>-pN-oM`. **Copy value**, **Copy key**, or **Copy headers** copies
the selected field as-is (headers are
serialized as JSON); it does not pretty-print JSON values. Clipboard actions,
record downloads, and JSON/CSV exports use the original data, not the truncated
preview. Exports are generated only when requested. Opening a large record in
the producer editor requires the explicit
**Open anyway** confirmation; cancelling does not open it. Show-more inspection
remains bounded and does not require that confirmation.
These are frontend rendering limits, not Kafka byte limits: polling, filters, and
pagination are unchanged. Full records still arrive through SSE and reside in
browser memory; arbitrarily large records or prolonged live consumption can still
exhaust memory.

## Links

* [Vite](https://github.com/vitejs/vite)
