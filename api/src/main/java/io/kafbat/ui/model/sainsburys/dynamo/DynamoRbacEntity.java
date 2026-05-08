package io.kafbat.ui.model.sainsburys.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "sainsburys-kafbat-rbac")
public class DynamoRbacEntity {
  @DynamoDBHashKey(attributeName = "partitionKey")
  private String name;
  @DynamoDBAttribute(attributeName = "clusters")
  private List<String> clusters;
  @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.L)
  @DynamoDBAttribute(attributeName = "subjects")
  private List<DynamoSubject> subjects;
  @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.L)
  @DynamoDBAttribute(attributeName = "permissions")
  private List<DynamoPermission> permissions;
  @DynamoDBAttribute(attributeName = "expirationTime")
  private Long expireTime;
}
