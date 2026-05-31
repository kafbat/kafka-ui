package io.kafbat.ui.exception;

public class OAuthTokenFetchException extends CustomBaseException {

  public OAuthTokenFetchException(String message) {
    super(message);
  }

  public OAuthTokenFetchException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.OAUTH_TOKEN_FETCH_ERROR;
  }
}
