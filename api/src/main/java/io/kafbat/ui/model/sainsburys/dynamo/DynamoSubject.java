package io.kafbat.ui.model.sainsburys.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.kafbat.ui.model.rbac.provider.Provider;
import lombok.Data;
import lombok.Getter;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Data
@DynamoDBDocument
public class DynamoSubject {

  @Getter(onMethod_ = {
      @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S),
      @DynamoDBAttribute(attributeName = "provider")
  })
  Provider provider;
  String type;
  String value;
  String createdTime;
  String expiryTime;
  @JsonProperty("isRegex")
  boolean isRegex;

  public void validate() {
    checkNotNull(type, "Subject type cannot be null");
    checkNotNull(value, "Subject value cannot be null");

    checkArgument(!type.isEmpty(), "Subject type cannot be empty");
    checkArgument(!value.isEmpty(), "Subject value cannot be empty");
  }

  public boolean matches(final String attribute) {
    if (isRegex) {
      return Objects.nonNull(attribute) && attribute.matches(this.value);
    }
    return this.value.equalsIgnoreCase(attribute);
  }
}
