package io.kafbat.ui.model.rbac;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@DynamoDBTable(tableName = "Rback_dummy")
public class DynamoRbacEntity {
  @DynamoDBHashKey(attributeName = "PK")
  private String name;
  @DynamoDBAttribute(attributeName = "clusters")
  private List<String> clusters;
  @DynamoDBAttribute(attributeName = "subjects")
  private List<Subject> subjects;
  @DynamoDBAttribute(attributeName = "permissions")
  private List<Permission> permissions;
}
