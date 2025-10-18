Feature: Produce Messages page
  Scenario: TopicName ui
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    When TopicCreate Topic name starts with: "ANewAutoTopic"
    When TopicCreate Number of partitons: 1
    When TopicCreate Time to retain data one day
    When TopicCreate Create topic clicked
    Then Header starts with: "ANewAutoTopic"
    Given Topics TopicName partitions is: 1
    Given Topics TopicName Overview visible is: "true"
    Given Topics TopicName Messages visible is: "true"
    Given Topics TopicName Consumers visible is: "true"
    Given Topics TopicName Settings visible is: "true"
    Given Topics TopicName Statistics visible is: "true"

  Scenario: Produce Message
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    When TopicCreate Topic name starts with: "ANewAutoTopic"
    When TopicCreate Number of partitons: 1
    When TopicCreate Time to retain data one day
    When TopicCreate Create topic clicked
    Then Header starts with: "ANewAutoTopic"
    Given Produce message clicked
    Then ProduceMessage header visible
    Given ProduceMessage Key input is: "keyFromAutotest"
    Given ProduceMessage Value input is: "ValueFromAutotest"
    Given ProduceMessage Headers input key is: "headerKey", value is: "headerValue"
    Given ProduceMessage Produce Message button clicked
    When Topics TopicName Messages clicked
    Then TopicName messages contains key: "keyFromAutotest"
    Then TopicName messages contains value: "ValueFromAutotest"
    Then TopicName messages contains headers key is: "headerKey", value is: "headerValue"

  Scenario: Topic Message cleanup policy
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    When TopicCreate Topic name starts with: "ANewAutoTopic"
    When TopicCreate Number of partitons: 1
    When TopicCreate Time to retain data one day
    When TopicCreate Create topic clicked
    Then Header starts with: "ANewAutoTopic"
    Given TopicName menu button clicked for topic starts with: "ANewAutoTopic"
    Then TopicNameMenu clear messages active is: "true"
    When TopicNameMenu edit settings clicked
    When TopicName cleanup policy set to: "Compact"
    When TopicName UpdateTopic button clicked
    Then Header starts with: "ANewAutoTopic"
    Given TopicName menu button clicked for topic starts with: "ANewAutoTopic"
    When TopicNameMenu edit settings clicked 
    When TopicName cleanup policy set to: "Delete"
    When TopicName UpdateTopic button clicked
    Then Header starts with: "ANewAutoTopic"
    Given TopicName menu button clicked for topic starts with: "ANewAutoTopic"
    Then TopicNameMenu clear messages active is: "true"

  Scenario: Produce messages clear messages
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    When TopicCreate Topic name starts with: "ANewAutoTopic"
    When TopicCreate Number of partitons: 1
    When TopicCreate Time to retain data one day
    When TopicCreate Create topic clicked
    Then Header starts with: "ANewAutoTopic"
    Given Produce message clicked
    Then ProduceMessage header visible
    Given ProduceMessage Key input is: "keyFromAutotest"
    Given ProduceMessage Value input is: "ValueFromAutotest"
    Given ProduceMessage Headers input key is: "headerKey", value is: "headerValue"
    Given ProduceMessage Produce Message button clicked
    When Topics TopicName Messages clicked
    Then TopicName messages contains key: "keyFromAutotest"
    Then TopicName messages contains value: "ValueFromAutotest"
    Then TopicName messages contains headers key is: "headerKey", value is: "headerValue"
    Then Topics TopicName Overview click
    Then TopicName messages count is "1"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest2"
    Given ProduceMessage Value input is: "ValueFromAutotest2"
    Given ProduceMessage Headers input key is: "headerKey2", value is: "headerValue2"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "2"
    When TopicName clear messages clicked
    Then TopicName messages count is "0"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest3"
    Given ProduceMessage Value input is: "ValueFromAutotest3"
    Given ProduceMessage Headers input key is: "headerKey3", value is: "headerValue3"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "1"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest4"
    Given ProduceMessage Value input is: "ValueFromAutotest4"
    Given ProduceMessage Headers input key is: "headerKey4", value is: "headerValue4"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "2"
    When TopicName menu button clicked for topic starts with: "ANewAutoTopic"
    When TopicName menu clear messages clicked
    Then TopicName messages count is "0"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest5"
    Given ProduceMessage Value input is: "ValueFromAutotest5"
    Given ProduceMessage Headers input key is: "headerKey5", value is: "headerValue5"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "1"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest6"
    Given ProduceMessage Value input is: "ValueFromAutotest6"
    Given ProduceMessage Headers input key is: "headerKey6", value is: "headerValue6"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "2"
    When TopicName menu button clicked for topic starts with: "ANewAutoTopic"
    When TopicName menu RecreateTopic clicked
    Then TopicName messages count is "0"
    Given Produce message clicked
    Given ProduceMessage Key input is: "keyFromAutotest7"
    Given ProduceMessage Value input is: "ValueFromAutotest7"
    Given ProduceMessage Headers input key is: "headerKey7", value is: "headerValue7"
    Given ProduceMessage Produce Message button clicked
    Then TopicName messages count is "1"

  
  Scenario: Topic message filter
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    When TopicCreate Topic name starts with: "ANewAutoTopic"
    When TopicCreate Number of partitons: 1
    When TopicCreate Time to retain data one day
    When TopicCreate Create topic clicked
    Then Header starts with: "ANewAutoTopic"
    Given Produce message clicked
    Then ProduceMessage header visible
    Given ProduceMessage Key input is: "keyFromAutotest"
    Given ProduceMessage Value input template Json
    Given ProduceMessage Headers input key is: "headerKey", value is: "headerValue"
    Given ProduceMessage Produce Message button clicked
    When Topics TopicName Messages clicked
    Given Topics TopicName AddFilters button click
    Given Topics TopicName AddFilter visible is: "true"
    Given Topics TopicName AddFilter filterCode Json value is: "2"
    Given Topics TopicName AddFilter display name starts with: "Filter"
    When Topics TopicName AddFilter button click
    Then Topics TopicName Messages filter name starts with: "Filter" visible is: "true"
    Then Topics TopicName Messages exist is: "true"

    Given Topics TopicName Messages edit filter button click
    Given Topics TopicName AddFilter filterCode change value is: "3"
    Then Topics TopicName AddFilter EditFilter button click

    Then Topics TopicName Messages filter name starts with: "Filter" visible is: "true"
    Then Topics TopicName Messages exist is: "false"
