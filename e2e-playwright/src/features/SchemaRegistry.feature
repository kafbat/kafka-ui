Feature: Schema page

  Scenario: SchemaRegistry and SchemaRegistryCreate visibility
    Given Schema Registry is visible
    When click on Schema Registry link
    Then Schema Registry heading visible
    Given SchemaRegistry CheateSchema clicked
    Given SchemaRegistryCreate is visible
    Given SchemaRegistryCreate Subject visible is: "true"
    Given SchemaRegistryCreate Schema visible is: "true"
    Given SchemaRegistryCreate SchemaType visible is: "true"
    When SchemaRegistryCreate Subject input starts with: "SchemaSubject"
    When SchemaRegistryCreate Schema input from avro
    When SchemaRegistryCreate Submit clicked
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "true"

  Scenario: SchemaRegistry Avro schema actions
    Given Schema Registry is visible
    When click on Schema Registry link
    Then Schema Registry heading visible
    Given SchemaRegistry CheateSchema clicked
    Given SchemaRegistryCreate is visible
    Given SchemaRegistryCreate Subject visible is: "true"
    Given SchemaRegistryCreate Schema visible is: "true"
    Given SchemaRegistryCreate SchemaType visible is: "true"
    When SchemaRegistryCreate Subject input starts with: "SchemaSubject"
    When SchemaRegistryCreate Schema input from avro
    When SchemaRegistryCreate Submit clicked
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "true"
    When click on Brokers link
    When click on Schema Registry link
    Given SchemaRegistry click on schema starts with: "SchemaSubject"
    Given SchemaRegistrySchemaName version is: "1"
    Given SchemaRegistrySchemaName type is: "AVRO"
    Given SchemaRegistrySchemaName Compatibility is: "BACKWARD"
    When SchemaRegistrySchemaName remove schema clicked
    Then Schema starts with: "SchemaSubject" deleted
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "false"

  Scenario: SchemaRegistry Json schema actions
    Given Schema Registry is visible
    When click on Schema Registry link
    Then Schema Registry heading visible
    Given SchemaRegistry CheateSchema clicked
    Given SchemaRegistryCreate is visible
    Given SchemaRegistryCreate Subject visible is: "true"
    Given SchemaRegistryCreate Schema visible is: "true"
    Given SchemaRegistryCreate SchemaType visible is: "true"
    When SchemaRegistryCreate Subject input starts with: "SchemaSubject"
    When SchemaRegistryCreate Schema input from json
    When SchemaRegistryCreate Submit clicked
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "true"
    When click on Brokers link
    When click on Schema Registry link
    Given SchemaRegistry click on schema starts with: "SchemaSubject"
    Given SchemaRegistrySchemaName version is: "1"
    Given SchemaRegistrySchemaName type is: "JSON"
    Given SchemaRegistrySchemaName Compatibility is: "BACKWARD"
    When SchemaRegistrySchemaName remove schema clicked
    Then Schema starts with: "SchemaSubject" deleted
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "false"

  Scenario: SchemaRegistry Json schema actions
    Given Schema Registry is visible
    When click on Schema Registry link
    Then Schema Registry heading visible
    Given SchemaRegistry CheateSchema clicked
    Given SchemaRegistryCreate is visible
    Given SchemaRegistryCreate Subject visible is: "true"
    Given SchemaRegistryCreate Schema visible is: "true"
    Given SchemaRegistryCreate SchemaType visible is: "true"
    When SchemaRegistryCreate Subject input starts with: "SchemaSubject"
    When SchemaRegistryCreate Schema input from protobuf
    When SchemaRegistryCreate Submit clicked
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "true"
    When click on Brokers link
    When click on Schema Registry link
    Given SchemaRegistry click on schema starts with: "SchemaSubject"
    Given SchemaRegistrySchemaName version is: "1"
    Given SchemaRegistrySchemaName type is: "PROTOBUF"
    Given SchemaRegistrySchemaName Compatibility is: "BACKWARD"
    When SchemaRegistrySchemaName remove schema clicked
    Then Schema starts with: "SchemaSubject" deleted
    Then SchemaRegistrySchemaName starts with: "SchemaSubject", visible is: "false"