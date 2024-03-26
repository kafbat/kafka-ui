package io.kafbat.ui.variables;

public interface Expected {

  String BROKER_SOURCE_INFO_TOOLTIP =
      "Dynamic topic config = dynamic topic config that is configured for a specific topic\n"
          + "Dynamic broker logger config = dynamic broker logger config that is configured for a specific broker\n"
          + "Dynamic broker config = dynamic broker config that is configured for a specific broker\n"
          + "Dynamic default broker config = dynamic broker config that is configured as default "
          + "for all brokers in the cluster\n"
          + "Static broker config = static broker config provided as broker properties at start up "
          + "(e.g. server.properties file)\n"
          + "Default config = built-in default configuration for configs that have a default value\n"
          + "Unknown = source unknown e.g. in the ConfigEntry used for alter requests where source is not set";
}
