Feature: TopicsCreate page

 Scenario: Topics Creating new Topic elemets
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