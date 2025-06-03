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
    Given TopicName menu button clicked
    Then TopicNameMenu clear messages active is: "true"

    When TopicNameMenu edit settings clicked
    When TopicName cleanup policy set to: "Compact"
    When TopicName UpdateTopic button clicked
    Then Header starts with: "ANewAutoTopic"
    Given TopicName menu button clicked
    Then TopicNameMenu clear messages active is: "false"

    When TopicNameMenu edit settings clicked
    When TopicName cleanup policy set to: "Delete"
    When TopicName UpdateTopic button clicked
    Then Header starts with: "ANewAutoTopic"
    Given TopicName menu button clicked
    Then TopicNameMenu clear messages active is: "true"