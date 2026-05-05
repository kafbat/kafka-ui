package io.kafbat.ui.model.sainsburys.confluent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagDefinitionClassificationResponse {
  private String category;
  private String createdBy;
  private String updatedBy;

  // Using long for epoch timestamps
  private long createTime;
  private long updateTime;

  private int version;
  private String name;
  private String description;
  private String typeVersion;

  private List<String> superTypes;
  private List<String> entityTypes;

  private String color;
}
