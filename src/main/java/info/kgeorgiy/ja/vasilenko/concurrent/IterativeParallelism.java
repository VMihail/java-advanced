package info.kgeorgiy.ja.vasilenko.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {
  /**
   * Нормализует количетво потоков
   * @param threadsCount исходное количество потоков
   * @param collectionLen длинна коллекции
   * @return {@code Math.min} от обоих аргументов
   */
  private int threadCountNormalization(final int threadsCount, final int collectionLen) {
    return Math.min(threadsCount, collectionLen);
  }

  /**
   * Разделяет список на заданное количество частей
   * @param values разделяемый список
   * @param numberOfParts количество частей, на которое мы его делим
   * @return Список из частей искомого списка
   * @param <T> тип содержимого списка
   */
  private <T> List<List<? extends T>> divide(final List<? extends T> values, int numberOfParts) {
    numberOfParts = threadCountNormalization(numberOfParts, values.size());
    final List<List<? extends T>> result = new ArrayList<>(numberOfParts);
    final int len = values.size() / numberOfParts;
    for (int i = 0; i < numberOfParts; i++) {
      final int left = i * len;
      if (i == numberOfParts - 1) {
        result.add(values.subList(left, values.size()));
      } else {
        result.add(values.subList(left, left + len));
      }
    }
    return result;
  }

  /**
   * Выполняет параллельно запрос для списка
   * @param threads количество потоков
   * @param values искомый список
   * @param function функция, которую мы вычисляем для списка
   * @param functionResult функция, которую мы вычисляет с результатами всех потоков
   * @return результат запроса
   * @param <T> тип объектов в списке
   * @param <R> тип результата запроса
   * @throws InterruptedException если произошла ошибка с потоками
   */
  private <T, R> R doParallel(int threads, final List<? extends T> values,
                           final Function<Stream<? extends T>, R> function,
                           final Function<List<R>, R> functionResult) throws InterruptedException {
    if (values.isEmpty()) {
      return null;
    }
    threads = threadCountNormalization(threads, values.size());
    final Thread[] threadsArray = new Thread[threads];
    final List<List<? extends T>> parts = divide(values, threads);
    final List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
    for (int i = 0; i < threads; i++) {
      final int currentInd = i;
      threadsArray[currentInd] = new Thread(() -> {
        result.set(currentInd, function.apply(parts.get(currentInd).stream()));
      });
      threadsArray[currentInd].start();
    }
    for (final var thread : threadsArray) {
      thread.join();
    }
    return functionResult.apply(result);
  }
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
    return doParallel(threads, values,
     (stream -> stream.max(comparator).orElse(null)),
     (resultList -> resultList.stream().max(comparator).orElse(null)));
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
    return doParallel(threads, values,
     (stream -> stream.min(comparator).orElse(null)),
     (resultList -> resultList.stream().min(comparator).orElse(null)));
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
    return Boolean.TRUE.equals(doParallel(threads, values,
     (stream -> stream.allMatch(predicate)),
     (resultList -> resultList.stream().allMatch(n -> n))));
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
    return !all(threads, values, predicate);
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
    return doParallel(threads, values,
     (stream -> (int) stream.filter(predicate).count()),
     (resultList -> resultList.stream().reduce(Integer::sum).orElse(0)));
  }
}
