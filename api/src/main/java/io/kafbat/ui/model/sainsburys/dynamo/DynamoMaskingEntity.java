package io.kafbat.ui.model.sainsburys.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "kit-kafbat-masking")
public class DynamoMaskingEntity {
  @DynamoDBHashKey(attributeName = "partitionKey")
  private String name;

  @DynamoDBAttribute(attributeName = "type")
  private String type;

  @DynamoDBAttribute(attributeName = "fields")
  private List<String> fields;

  @DynamoDBAttribute(attributeName = "fieldsNamePattern")
  private String fieldsNamePattern;

  @DynamoDBAttribute(attributeName = "replacement")
  private String replacement; // Used if type is REPLACE

  @DynamoDBAttribute(attributeName = "maskingCharsReplacement")
  private List<String> maskingCharsReplacement;

  @DynamoDBAttribute(attributeName = "topicKeysPattern")
  private String topicKeysPattern;

  @DynamoDBAttribute(attributeName = "topicValuesPattern")
  private String topicValuesPattern;
}
