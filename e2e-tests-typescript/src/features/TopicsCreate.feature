Feature: TopicsCreate page

 Scenario: TopicCreate elemets visible
    Given Topics is visible
    When click on Topics link
    Given Topics AddATopic clicked
    Given TopicCreate heading visible is: "true"
    Given TopicCreate TopicName input visible is: "true"
    Given TopicCreate NumberOfPartitions input visible is: "true"
    Given TopicCreate CleanupPolicy select visible is: "true"
    Given TopicCreate MinInSyncReplicas input visible is: "true"
    Given TopicCreate ReplicationFactor input visible is: "true"
    Given TopicCreate TimeToRetainData input visible is: "true"
    Given TopicCreate 12Hours button visible is: "true"
    Given TopicCreate 1Day button visible is: "true"
    Given TopicCreate 2Day button visible is: "true"
    Given TopicCreate 7Day button visible is: "true"
    Given TopicCreate 4Weeks button visible is: "true"
    Given TopicCreate MaxPartitionSize select visible is: "true"
    Given TopicCreate MaxMessageSize input visible is: "true"
    Given TopicCreate AddCustomParameter button visible is: "true"
    Given TopicCreate Cancel button visible is: "true"
    Given TopicCreate CreateTopic button visible is: "true"

Scenario: TopicCreate ui functions
   Given Topics is visible
   When click on Topics link
   Given Topics AddATopic clicked
   Given TopicCreate heading visible is: "true"
   When TopicCreate Topic name starts with: "NewAutoTopic"
   When TopicCreate Number of partitons: 2
   When TopicCreate Time to retain data one day
   When TopicCreate Create topic clicked
   Then Header starts with: "NewAutoTopic"
   When click on Topics link
   Then Topic name started with: "NewAutoTopic" visible is: "true"