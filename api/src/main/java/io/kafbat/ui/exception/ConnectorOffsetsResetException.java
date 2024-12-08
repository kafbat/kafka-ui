package io.kafbat.ui.exception;

public class ConnectorOffsetsResetException extends CustomBaseException {

  public ConnectorOffsetsResetException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.CONNECTOR_OFFSETS_RESET_ERROR;
  }
}
