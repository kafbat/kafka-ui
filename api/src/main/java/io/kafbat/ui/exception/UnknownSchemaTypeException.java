package io.kafbat.ui.exception;

public class UnknownSchemaTypeException extends IllegalStateException {
  public UnknownSchemaTypeException(String type) {
    super("Unknown schema type: " + type);
  }
}
