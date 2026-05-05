package io.kafbat.ui.model.sainsburys.confluent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityAttributes {

  private @Nullable Long createTime;

  private @Nullable String qualifiedName;

  private @Nullable String name;

  private @Nullable String context;

  private @Nullable Integer id;

  private @Nullable String nameLower;

  private @Nullable String tenant;

}

