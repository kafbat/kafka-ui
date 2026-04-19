Feature: KafkaConnect page visibility and functions

  Scenario: KafkaConnect search is working
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given KafkaConnect cell element "first" is clicked
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    Given KafkaConnect with name: "s3-sink" visible is: "true"
    When KafkaConnect searchfield input is: "sink_postgres"
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    Given KafkaConnect with name: "s3-sink" visible is: "false" 

  Scenario: KafkaConnect main page functions
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given KafkaConnect cell element "first" is clicked
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    Given KafkaConnect with name: "s3-sink" visible is: "true"
    When KafkaConnect searchfield input is: "sink_postgres"
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    Given KafkaConnect with name: "s3-sink" visible is: "false"
    Given KafkaConnect satus is: "RUNNING", type is: "SINK"
    Given KafkaConnect row menu menu item "Stop" is clicked
    Given KafkaConnect satus is: "STOPPED", type is: "SINK"
    Given KafkaConnect row menu menu item "Resume" is clicked
    Given KafkaConnect satus is: "RUNNING", type is: "SINK"
    Given KafkaConnect row menu menu item "Pause" is clicked
    Given KafkaConnect satus is: "PAUSED", type is: "SINK"
    Given KafkaConnect row menu menu item "Resume" is clicked
    Given KafkaConnect satus is: "RUNNING", type is: "SINK"

  Scenario: KafkaConnect connector page
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given KafkaConnect cell element "first" is clicked
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    When KafkaConnect connector named: "sink_postgres_activities" clicked
    Then KafkaConnect connector page with label: "KafkaConnectsink_postgres_activities" open

  Scenario: KafkaConnect connector page functions
    Given Kafka Connect is visible
    When click on Kafka Connect link
    Then Kafka Connect heading visible
    Given KafkaConnect cell element "first" is clicked
    Given KafkaConnect with name: "sink_postgres_activities" visible is: "true"
    When KafkaConnect connector named: "sink_postgres_activities" clicked
    Then KafkaConnect connector page with label: "KafkaConnectsink_postgres_activities" open
    Given KafkaConnect connector page status is: "RUNNING"
    When KafkaConnect connector menu item "Pause" clicked
    Given KafkaConnect connector page status is: "PAUSED"
    When KafkaConnect connector menu item "Resume" clicked
    Given KafkaConnect connector page status is: "RUNNING"
    When KafkaConnect connector menu item "Stop" clicked
    Given KafkaConnect connector page state is: "STOPPED"
    When KafkaConnect connector menu item "Resume" clicked