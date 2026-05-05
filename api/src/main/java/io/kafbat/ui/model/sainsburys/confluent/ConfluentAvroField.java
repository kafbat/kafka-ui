package io.kafbat.ui.model.sainsburys.confluent;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluentAvroField {
  private String name;
  private Object type;

  // This map will hold "example.tag", "description", etc.
  private Map<String, Object> customTags = new HashMap<>();

  @JsonAnySetter
  public void addCustomTag(String key, Object value) {
    this.customTags.put(key, value);
  }
}
