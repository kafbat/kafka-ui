Feature: Connectors page visibility and functions

  Scenario: Connectors search is working
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    Given Connectors with name: "s3-sink" visible is: "true"
    When Connectors searchfield input is: "sink_postgres"
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    Given Connectors with name: "s3-sink" visible is: "false" 

  Scenario: Connectors main page functions
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    Given Connectors with name: "s3-sink" visible is: "true"
    When Connectors searchfield input is: "sink_postgres"
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    Given Connectors with name: "s3-sink" visible is: "false"
    Given Connectors satus is: "RUNNING", type is: "SINK"
    Given Connectors row menu menu item "Stop" is clicked
    Given Connectors satus is: "STOPPED", type is: "SINK"
    Given Connectors row menu menu item "Resume" is clicked
    Given Connectors satus is: "RUNNING", type is: "SINK"
    Given Connectors row menu menu item "Pause" is clicked
    Given Connectors satus is: "PAUSED", type is: "SINK"
    Given Connectors row menu menu item "Resume" is clicked
    Given Connectors satus is: "RUNNING", type is: "SINK"

  Scenario: Connectors connector page
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    When Connectors connector named: "sink_postgres_activities" clicked
    Then Connectors connector page with label: "Connectorssink_postgres_activities" open

  Scenario: Connectors connector page functions
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given Connectors with name: "sink_postgres_activities" visible is: "true"
    When Connectors connector named: "sink_postgres_activities" clicked
    Then Connectors connector page with label: "Connectorssink_postgres_activities" open
    Given Connectors connector page status is: "RUNNING"
    When Connectors connector menu item "Pause" clicked
    Given Connectors connector page status is: "PAUSED"
    When Connectors connector menu item "Resume" clicked
    Given Connectors connector page status is: "RUNNING"
    When Connectors connector menu item "Stop" clicked
    Given Connectors connector page state is: "STOPPED"
    When Connectors connector menu item "Resume" clicked