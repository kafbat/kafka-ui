package io.kafbat.ui.model.sainsburys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaMetadataResponse {

  private @Nullable SearchParameters searchParameters;

  @Valid
  private List<String> types = new ArrayList<>();

  @Valid
  private List<@Valid Entity> entities = new ArrayList<>();
}
