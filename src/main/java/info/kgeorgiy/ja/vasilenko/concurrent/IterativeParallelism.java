package info.kgeorgiy.ja.vasilenko.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * impl of {@link ListIP}
 */
public class IterativeParallelism implements ListIP {
  private static final String INTERRUPTED_EXCEPTION_MESSAGE = "Executing thread was interrupted";

  /**
   * Returns maximum value.
   *
   * @param threads    number of concurrent threads.
   * @param values     values to get maximum of.
   * @param comparator value comparator.
   * @return maximum of given values
   * @throws InterruptedException   if executing thread was interrupted.
   * @throws NoSuchElementException if no values are given.
   */
  @Override
  public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     (stream -> stream.max(comparator).orElse(null)),
     (stream -> stream.max(comparator).orElse(null))
    );
  }

  /**
   * Returns minimum value.
   *
   * @param threads    number of concurrent threads.
   * @param values     values to get minimum of.
   * @param comparator value comparator.
   * @return minimum of given values
   * @throws InterruptedException   if executing thread was interrupted.
   * @throws NoSuchElementException if no values are given.
   */
  @Override
  public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     (stream -> stream.min(comparator).orElse(null)),
     (stream -> stream.min(comparator).orElse(null))
    );
  }

  /**
   * Returns whether all values satisfy predicate.
   *
   * @param threads   number of concurrent threads.
   * @param values    values to test.
   * @param predicate test predicate.
   * @return whether all values satisfy predicate or {@code true}, if no values are given.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     (stream -> stream.allMatch(predicate)),
     (stream -> stream.allMatch(result -> result))
    );
  }

  /**
   * Returns whether any of values satisfies predicate.
   *
   * @param threads   number of concurrent threads.
   * @param values    values to test.
   * @param predicate test predicate.
   * @return whether any value satisfies predicate or {@code false}, if no values are given.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     (stream -> stream.anyMatch(predicate)),
     (stream -> stream.anyMatch(result -> result))
    );
  }

  /**
   * Returns number of values satisfying predicate.
   *
   * @param threads   number of concurrent threads.
   * @param values    values to test.
   * @param predicate test predicate.
   * @return number of values satisfying predicate.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
    threads = threadsCountNormalize(threads, values);
    final var parts = divideIntoParts(values, threads);
    final List<Integer> results = new ArrayList<>(Collections.nCopies(threads, null));
    final Thread[] threadArray = new Thread[threads];
    for (int i = 0; i < threads; i++) {
      final int index = i;
      final Thread thread = new Thread(() -> results.set(index, (int) parts.get(index).stream().filter(predicate).count()));
      threadArray[i] = thread;
      thread.start();
    }
    Arrays.stream(threadArray).forEach((thread -> {
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new RuntimeException(INTERRUPTED_EXCEPTION_MESSAGE, e);
      }
    }));
    return results.stream().reduce(0, Integer::sum);
  }

  /**
   * Join values to string.
   *
   * @param threads number of concurrent threads.
   * @param values  values to join.
   * @return list of joined results of {@link Object#toString()} call on each value.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public String join(int threads, List<?> values) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     stream -> stream.map(Object::toString).collect(Collectors.joining()),
     stream -> stream.collect(Collectors.joining())
    );
  }

  /**
   * Filters values by predicate.
   *
   * @param threads   number of concurrent threads.
   * @param values    values to filter.
   * @param predicate filter predicate.
   * @return list of values satisfying given predicate. Order of values is preserved.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     stream -> stream.filter(predicate).collect(Collectors.toList()),
     stream -> stream.flatMap(List::stream).toList()
    );
  }

  /**
   * Maps values.
   *
   * @param threads number of concurrent threads.
   * @param values  values to map.
   * @param f       mapper function.
   * @return list of values mapped by given function.
   * @throws InterruptedException if executing thread was interrupted.
   */
  @Override
  public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
    return executeParallelAction(
     threads,
     values,
     stream -> stream.map(f).collect(Collectors.toList()),
     stream -> stream.flatMap(List::stream).toList()
    );
  }

  /**
   * Performs a multi-threaded action.
   * @param threads number of threads
   * @param values processed List
   * @param calculateFunction function that each thread executes
   * @param resultCalculateFunction function to find the overall result
   * @return result of two functions, type - U
   * @param <T> type of list elements
   * @param <U> type of result
   */
  private <T, U> U executeParallelAction(
   int threads,
   List<? extends T> values,
   final Function<Stream<? extends T>, ? extends U> calculateFunction,
   final Function<Stream<? extends U>, ? extends U> resultCalculateFunction
  ) {
    threads = threadsCountNormalize(threads, values);
    final var parts = divideIntoParts(values, threads);
    final List<U> results = new ArrayList<>(Collections.nCopies(threads, null));
    final Thread[] threadArray = new Thread[threads];
    for (int i = 0; i < threads; i++) {
      final int index = i;
      final Thread thread = new Thread(() -> results.set(index, calculateFunction.apply(parts.get(index).stream())));
      threadArray[i] = thread;
      thread.start();
    }
    Arrays.stream(threadArray).forEach((thread -> {
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw new RuntimeException(INTERRUPTED_EXCEPTION_MESSAGE, e);
      }
    }));
    return resultCalculateFunction.apply(results.stream());
  }

  /**
   * Splits a list into multiple parts.
   * @param list processed List
   * @param partsCount number of parts
   * @return list of sublists
   * @param <T> type of list elements
   */
  private <T> List<List<? extends T>> divideIntoParts(final List<? extends T> list, final int partsCount) {
    final List<List<? extends T>> result = new ArrayList<>();
    final int wholePart = list.size() / partsCount;
    int remains = list.size() % partsCount;
    int left = 0;
    for (int i = 0; i < partsCount; i++) {
      int right = left + wholePart + (remains-- > 0 ? 1 : 0);
      result.add(list.subList(left, right));
      left = right;
    }
    return result;
  }

  /**
   * Normalizes the number of threads.
   * @param threadsCount number of threads
   * @param list processed List
   * @return {@code min(threadsCount, list.size())}
   * @param <T> type of list elements
   */
  private <T> int threadsCountNormalize(final int threadsCount, final List<? extends T> list) {
    return Math.min(threadsCount, list.size());
  }
}
