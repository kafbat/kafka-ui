package io.kafbat.ui.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Connector {

  private String name, config;
}
