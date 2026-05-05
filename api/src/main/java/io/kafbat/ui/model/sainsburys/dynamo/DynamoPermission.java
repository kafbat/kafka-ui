package io.kafbat.ui.model.sainsburys.dynamo;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@DynamoDBDocument
public class DynamoPermission {

  String resource;

  List<String> actions;

  @Nullable
  String value;

  public void validate() {
    Preconditions.checkNotNull(resource, "resource cannot be null");
    Preconditions.checkArgument(isNotEmpty(actions), "Actions list for %s can't be null or empty", resource);
  }

}
