package io.kafbat.ui.variables;

public interface Common {

  String LOG_RESULT = "-> {}";
  String BROKER_SOURCE_INFO_TOOLTIP =
      """
          Dynamic topic config = dynamic topic config that is configured for a specific topic
          Dynamic broker logger config = dynamic broker logger config that is configured for a specific broker
          Dynamic broker config = dynamic broker config that is configured for a specific broker
          Dynamic default broker config = dynamic broker config that is configured as default \
          for all brokers in the cluster
          Static broker config = static broker config provided as broker properties at start up \
          (e.g. server.properties file)
          Default config = built-in default configuration for configs that have a default value
          Unknown = source unknown e.g. in the ConfigEntry used for alter requests where source is not set""";
  String FILTER_CODE_STRING = "has(record.keyAsText) && record.keyAsText.matches(\".*[Gg]roovy.*\")";
  String FILTER_CODE_JSON = "has(record.key.name.first) && record.key.name.first == 'user1'";
}
