package io.kafbat.ui.exception;

public class MessageViewingDisabledException extends CustomBaseException {

  public MessageViewingDisabledException() {
    super("Message viewing is disabled for this cluster");
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.MESSAGE_VIEWING_DISABLED;
  }
}
