Feature: Topics page visibility and functions

  Scenario: Topics elements
    Given Topics is visible
    When click on Topics link
    Given Topics serchfield visible
    And Topics ShowInternalTopics visible 
    And Topics AddATopic visible
    And Topics DeleteSelectedTopics active is: "false"
    And Topics CopySelectedTopic active is: "false"
    And Topics PurgeMessagesOfSelectedTopics active is: "false"
    When Topic SelectAllTopic visible is: "true"
    Then Topic SelectAllTopic checked is: "true"
    Given Topics DeleteSelectedTopics active is: "true"
    And Topics CopySelectedTopic active is: "false"
    And Topics PurgeMessagesOfSelectedTopics active is: "true"
    Then Topic SelectAllTopic checked is: "false"
    Given Topics DeleteSelectedTopics active is: "false"
    And Topics CopySelectedTopic active is: "false"
    And Topics PurgeMessagesOfSelectedTopics active is: "false"
    When Topics serchfield input "SomeTopic"
    Then Topic named: "SomeTopic" visible is: "true"
    When Topic serchfield input cleared
    Then Topic named: "SomeTopic" visible is: "true"
    When Topic row named: "SomeTopic" checked is: "true"
    Given Topics DeleteSelectedTopics active is: "true"
    And Topics CopySelectedTopic active is: "true"
    And Topics PurgeMessagesOfSelectedTopics active is: "true" 
    When Topic row named: "SomeTopic" checked is: "false"
    Given Topics DeleteSelectedTopics active is: "false"
    And Topics CopySelectedTopic active is: "false"
    And Topics PurgeMessagesOfSelectedTopics active is: "false"

 Scenario: Topics serchfield and ShowInternalTopics
    Given Topics is visible
    When click on Topics link
    And Topics serchfield visible
    When Topics serchfield input "__consumer_offsets"
    Then Topic named: "__consumer_offsets" visible is: "true"
    When Topics ShowInternalTopics switched is: "false"
    Then Topic named: "__consumer_offsets" visible is: "false"
    When Topics ShowInternalTopics switched is: "true"
    Then Topic named: "__consumer_offsets" visible is: "true"
    When Topics serchfield input "SomeTopic"
    Then Topic named: "SomeTopic" visible is: "true"

