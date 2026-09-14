package io.kafbat.ui.exception;

public class InternalTopicModificationException extends CustomBaseException {

  public InternalTopicModificationException(String topicName) {
    super(String.format("Topic '%s' is internal and can not be modified.", topicName));
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.INTERNAL_TOPIC_MODIFICATION;
  }
}
