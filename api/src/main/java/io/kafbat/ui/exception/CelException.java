package io.kafbat.ui.exception;

public class CelException extends CustomBaseException {
  private final String celOriginalExpression;

  public CelException(String celOriginalExpression, String errorMessage) {
    super("CEL error. Original expression: %s. Error message: %s".formatted(celOriginalExpression, errorMessage));

    this.celOriginalExpression = celOriginalExpression;
  }

  public CelException(String celOriginalExpression, Throwable celThrowable) {
    super("CEL error. Original expression: %s".formatted(celOriginalExpression), celThrowable);

    this.celOriginalExpression = celOriginalExpression;
  }

  @Override
  public ErrorCode getErrorCode() {
    return ErrorCode.CEL_ERROR;
  }
}
