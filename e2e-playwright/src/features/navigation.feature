Feature: Navigation panel links

  Scenario: Navigate to Brokers
    Given Brokers is visible
    When click on Brokers link
    Then Brokers heading visible
    Then the end of current URL should be "brokers"

  Scenario: Navigate to Topics
    Given Topics is visible
    When click on Topics link
    Then Topics heading visible
    Then the part of current URL should be "topics"

  Scenario: Navigate to Consumers
    Given Consumers is visible
    When click on Consumers link
    Then Consumers heading visible
    Then the end of current URL should be "consumer-groups"

  Scenario: Navigate to Schema Registry
    Given Schema Registry is visible
    When click on Schema Registry link
    Then Schema Registry heading visible
    Then the end of current URL should be "schemas"

  Scenario: Navigate to Kafka Connect
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Then the end of current URL should be "clusters"

  Scenario: Navigate to KSQL DB
    Given KSQL DB is visible
    When click on KSQL DB link
    Then KSQL DB heading visible
    Then the part of current URL should be "ksqldb"

  Scenario: Navigate to Dashboard
    Given Dashboard is visible
    When click on Dashboard link
    Then Dashboard heading visible
