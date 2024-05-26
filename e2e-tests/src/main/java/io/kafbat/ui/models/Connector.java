package io.kafbat.ui.models;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Connector {

  private String name, config;

  public static Connector createConnector(String config) {
    return new Connector()
        .setName("aqa_connector_" + randomAlphabetic(5))
        .setConfig(config);
  }
}
