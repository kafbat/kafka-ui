Feature: Ksqldb page visibility

  Scenario: KSQL DB elements visibility
    Given KSQL DB is visible
    When click on KSQL DB link
    Then KSQL DB heading visible
    Then the part of current URL should be "ksqldb"
    Given KSQL DB Tables header visible is: "true"
    Given KSQL DB Streams header visible is: "true"
    Given KSQL DB Tables link visible is: "true"
    Given KSQL DB Streams link visible is: "true"
    When KSQL DB ExecuteKSQLRequest click
    Given KSQL DB Clear visible is: "true"
    Given KSQL DB Execute visible is: "true"

  Scenario: KSQL DB queries clear result
    Given KSQL DB is visible
    When click on KSQL DB link
    Then KSQL DB heading visible
    Then the part of current URL should be "ksqldb"
    Given KSQL DB Tables header visible is: "true"
    Given KSQL DB Streams header visible is: "true"
    Given KSQL DB Tables link visible is: "true"
    Given KSQL DB Streams link visible is: "true"
    When KSQL DB ExecuteKSQLRequest click
    Given KSQL DB textbox visible is: "true"
    Given KSQL DB KSQL for stream starts with: "STREAM_ONE", kafka_topic starts with: "NewAutoTopic", value_format: "json"
    Then KSQL DB stream created
    Then KSQL DB clear result visible is: "true"

  Scenario: KSQL DB queries
    Given KSQL DB is visible
    When click on KSQL DB link
    Then KSQL DB heading visible
    Then the part of current URL should be "ksqldb"
    Given KSQL DB Tables header visible is: "true"
    Given KSQL DB Streams header visible is: "true"
    Given KSQL DB Tables link visible is: "true"
    Given KSQL DB Streams link visible is: "true"

    When KSQL DB ExecuteKSQLRequest click
    Given KSQL DB textbox visible is: "true"
    Given KSQL DB KSQL for stream starts with: "STREAM_ONE", kafka_topic starts with: "NewAutoTopic", value_format: "json"
    Then KSQL DB stream created
    When KSQL DB Stream clicked
    Then KSQL DB stream starts with: "STREAM_ONE" visible is: "true"

    When KSQL DB ExecuteKSQLRequest click
    Given KSQL DB textbox visible is: "true"
    Then KSQL DB KSQL for table starts with: "TABLE_ONE", stream starts with: "STREAM_ONE"
    Then KSQL DB table created
    When KSQL DB Table clicked
    Then KSQL DB table starts with: "TABLE_ONE" visible is: "true"

