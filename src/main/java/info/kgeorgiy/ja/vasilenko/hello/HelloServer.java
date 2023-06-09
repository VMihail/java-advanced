package info.kgeorgiy.ja.vasilenko.hello;

public interface HelloServer extends AutoCloseable {
  /**
   * Starts a new Hello server.
   * This method should return immediately.
   *
   * @param port    server port.
   * @param threads number of working threads.
   */
  void start(int port, int threads);

  /**
   * Stops server and deallocates all resources.
   */
  @Override
  void close();
}
