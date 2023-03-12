package info.kgeorgiy.ja.vasilenko.implementor.tools;

/**
 * @author VMihail (vmihail399@gmail.com)
 */
public class ImplerException extends RuntimeException {
  public ImplerException(final String message) {
    super(message);
  }

  public ImplerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
