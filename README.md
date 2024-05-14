<div align="center">
<img src="documentation/images/logo_new.png" alt="logo"/>
<h3>Kafbat UI</h3>

Versatile, fast and lightweight web UI for managing Apache Kafka® clusters.
</div>

<div align="center">
<a href="https://github.com/kafbat/kafka-ui/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/></a>
<img src="documentation/images/free-open-source.svg" alt="price free"/>
<a href="https://github.com/kafbat/kafka-ui/releases"><img src="https://img.shields.io/github/v/release/kafbat/kafka-ui" alt="latest release version"/></a>
<a href="https://discord.gg/4DWzD7pGE5"><img src="https://img.shields.io/discord/897805035122077716" alt="discord online number count"/></a>
<a href="https://github.com/sponsors/kafbat"><img src="https://img.shields.io/github/sponsors/kafbat?style=flat&logo=githubsponsors&logoColor=%23EA4AAA&label=Support%20us" alt="" /></a>
</div>

<p align="center">
    <a href="https://ui.docs.kafbat.io/">Documentation</a> • 
    <a href="https://ui.docs.kafbat.io/configuration/quick-start">Quick Start</a> • 
    <a href="https://discord.gg/4DWzD7pGE5">Community</a>
    <br/>
    <a href="https://aws.amazon.com/marketplace/pp/{replaceMe}">AWS Marketplace</a>  •
    <a href="https://www.producthunt.com/products/ui-for-apache-kafka/reviews/new">ProductHunt</a>
</p>

<p align="center">
  <img src="https://repobeats.axiom.co/api/embed/88d2bd9887380c7d86e2f986725d9af52ebad7f4.svg" alt="stats"/>
</p>

#### Kafbat UI is a free, open-source web UI to monitor and manage Apache Kafka clusters.

Kafbat UI is a simple tool that makes your data flows observable, helps find and troubleshoot issues faster and deliver optimal performance. Its lightweight dashboard makes it easy to track key metrics of your Kafka clusters - Brokers, Topics, Partitions, Production, and Consumption.

<i>
Kafbat UI, developed by <b>Kafbat</b>*, proudly carries forward the legacy of the UI Apache Kafka project.
Our dedication is reflected in the continuous evolution of the project, ensuring adherence to its foundational vision while adapting to meet modern demands.
We extend our gratitude to Provectus for their past support in groundbreaking work, which serves as a cornerstone for our ongoing innovation and dedication.

<b>*</b> - The <b>Kafbat</b> team comprises key contributors from the project's inception, bringing a wealth of experience and insight to this renewed endeavor.
</i>

# Interface

![Interface](https://raw.githubusercontent.com/kafbat/kafka-ui/images/overview.gif)

# Features
* **Multi-Cluster Management** — monitor and manage all your clusters in one place
* **Performance Monitoring with Metrics Dashboard** —  track key Kafka metrics with a lightweight dashboard
* **View Kafka Brokers** — view topic and partition assignments, controller status
* **View Kafka Topics** — view partition count, replication status, and custom configuration
* **View Consumer Groups** — view per-partition parked offsets, combined and per-partition lag
* **Browse Messages** — browse messages with JSON, plain text, and Avro encoding
* **Dynamic Topic Configuration** — create and configure new topics with dynamic configuration
* **Configurable Authentification** — [secure](https://ui.docs.kafbat.io/configuration/authentication) your installation with optional Github/Gitlab/Google OAuth 2.0
* **Custom serialization/deserialization plugins** - [use](https://ui.docs.kafbat.io/configuration/serialization-serde) a ready-to-go serde for your data like AWS Glue or Smile, or code your own!
* **Role based access control** - [manage permissions](https://ui.docs.kafbat.io/configuration/rbac-role-based-access-control) to access the UI with granular precision
* **Data masking** - [obfuscate](https://ui.docs.kafbat.io/configuration/data-masking) sensitive data in topic messages

## Feature overview

<details>
    <summary>Click here for the feature overview</summary>

# The Interface
Kafbat UI wraps major functions of Apache Kafka with an intuitive user interface.

![Interface](documentation/images/Interface.gif)

## Topics
Kafbat UI makes it easy for you to create topics in your browser by several clicks,
pasting your own parameters, and viewing topics in the list.

![Create Topic](documentation/images/Create_topic_kafka-ui.gif)

It's possible to jump from connectors view to corresponding topics and from a topic to consumers (back and forth) for more convenient navigation.
connectors, overview topic settings.

![Connector_Topic_Consumer](documentation/images/Connector_Topic_Consumer.gif)

### Messages
Let's say we want to produce messages for our topic. With the Kafbat UI we can send or write data/messages to the Kafka topics without effort by specifying parameters, and viewing messages in the list.

![Produce Message](documentation/images/Create_message_kafka-ui.gif)

## Schema registry
There are 3 supported types of schemas: Avro®, JSON Schema, and Protobuf schemas.

![Create Schema Registry](documentation/images/Create_schema.gif)

Before producing avro/protobuf encoded messages, you have to add a schema for the topic in Schema Registry. Now all these steps are easy to do
with a few clicks in a user-friendly interface.

![Avro Schema Topic](documentation/images/Schema_Topic.gif)

</details>

# Getting Started

To run Kafbat UI, you can use either a pre-built Docker image or build it (or a jar file) yourself.

## Quick start (Demo run)

```
docker run -it -p 8080:8080 -e DYNAMIC_CONFIG_ENABLED=true ghcr.io/kafbat/kafka-ui
```

Then access the web UI at [http://localhost:8080](http://localhost:8080)

The command is sufficient to try things out. When you're done trying things out, you can proceed with a [persistent installation](https://ui.docs.kafbat.io/quick-start/persistent-start)

## Persistent installation

```
services:
  kafbat-ui:
    container_name: kafbat-ui
    image: ghcr.io/kafbat/kafka-ui:latest
    ports:
      - 8080:8080
    environment:
      DYNAMIC_CONFIG_ENABLED: 'true'
    volumes:
      - ~/kui/config.yml:/etc/kafkaui/dynamic_config.yaml
```

Please refer to our [configuration](https://ui.docs.kafbat.io/configuration/configuration-file) page to proceed with further app configuration.

## Some useful configuration related links

[Web UI Cluster Configuration Wizard](https://ui.docs.kafbat.io/configuration/configuration-wizard)

[Configuration file explanation](https://ui.docs.kafbat.io/configuration/configuration-file)

[Docker Compose examples](https://ui.docs.kafbat.io/configuration/compose-examples)

[Misc configuration properties](https://ui.docs.kafbat.io/configuration/misc-configuration-properties)

## Helm charts

[Quick start](https://ui.docs.kafbat.io/configuration/helm-charts/quick-start)

## Building from sources

[Quick start](https://ui.docs.kafbat.io/development/building/prerequisites) with building

## Liveliness and readiness probes
Liveliness and readiness endpoint is at `/actuator/health`.<br/>
Info endpoint (build info) is located at `/actuator/info`.

# Configuration options

All the environment variables/config properties could be found [here](https://ui.docs.kafbat.io/configuration/misc-configuration-properties).

# Contributing

Please refer to [contributing guide](https://ui.docs.kafbat.io/development/contributing), we'll guide you from there.

# Support

As we're fully independent, team members contribute in their free time.
Your support is crucial for us, if you wish to sponsor us, take a look [here](https://github.com/sponsors/kafbat) 
