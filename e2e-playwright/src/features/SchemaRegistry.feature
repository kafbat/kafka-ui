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