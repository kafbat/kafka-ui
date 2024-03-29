package io.kafbat.ui.model.schemaregistry;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.kafbat.ui.model.SchemaTypeDTO;
import lombok.Data;

@Data
public class InternalNewSchema {
  private String schema;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SchemaTypeDTO schemaType;

  public InternalNewSchema(String schema, SchemaTypeDTO schemaType) {
    this.schema = schema;
    this.schemaType = schemaType;
  }
}
