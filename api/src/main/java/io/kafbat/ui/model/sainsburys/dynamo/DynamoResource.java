package io.kafbat.ui.model.sainsburys.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.Data;
import java.util.List;

@Data
@DynamoDBDocument
public class DynamoResource {
  String type;
  String name;
  List<String> actions;

}
