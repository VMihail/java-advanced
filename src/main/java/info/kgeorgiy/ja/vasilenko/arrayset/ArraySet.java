package info.kgeorgiy.ja.vasilenko.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
  private final List<T> list;
  private final Comparator<T> comparator;

  public ArraySet(final Comparator<T> comparator) {
    this.comparator = Objects.requireNonNull(comparator);
    this.list = new ArrayList<>();
  }

  public ArraySet(final Collection<T> collection, final Comparator<T> comparator) {
    this(comparator);
    list.addAll(collection);
    list.sort(comparator);
  }

  private ArraySet(final List<T> list, final Comparator<T> comparator) {
    this.comparator = Objects.requireNonNull(comparator);
    this.list = Objects.requireNonNull(list);
  }

  /**
   * Returns an iterator over the elements contained in this collection.
   *
   * @return an iterator over the elements contained in this collection
   */
  @Override
  public Iterator<T> iterator() {
    return list.iterator();
  }

  @Override
  public int size() {
    return list.size();
  }

  /**
   * Returns the comparator used to order the elements in this set,
   * or {@code null} if this set uses the {@linkplain Comparable
   * natural ordering} of its elements.
   *
   * @return the comparator used to order the elements in this set,
   * or {@code null} if this set uses the natural ordering
   * of its elements
   */
  @Override
  public Comparator<? super T> comparator() {
    return comparator;
  }

  /**
   * Returns a view of the portion of this set whose elements range
   * from {@code fromElement}, inclusive, to {@code toElement},
   * exclusive.  (If {@code fromElement} and {@code toElement} are
   * equal, the returned set is empty.)  The returned set is backed
   * by this set, so changes in the returned set are reflected in
   * this set, and vice-versa.  The returned set supports all
   * optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException}
   * on an attempt to insert an element outside its range.
   *
   * @param fromElement low endpoint (inclusive) of the returned set
   * @param toElement   high endpoint (exclusive) of the returned set
   * @return a view of the portion of this set whose elements range from
   * {@code fromElement}, inclusive, to {@code toElement}, exclusive
   * @throws ClassCastException       if {@code fromElement} and
   *                                  {@code toElement} cannot be compared to one another using this
   *                                  set's comparator (or, if the set has no comparator, using
   *                                  natural ordering).  Implementations may, but are not required
   *                                  to, throw this exception if {@code fromElement} or
   *                                  {@code toElement} cannot be compared to elements currently in
   *                                  the set.
   * @throws NullPointerException     if {@code fromElement} or
   *                                  {@code toElement} is null and this set does not permit null
   *                                  elements
   * @throws IllegalArgumentException if {@code fromElement} is
   *                                  greater than {@code toElement}; or if this set itself
   *                                  has a restricted range, and {@code fromElement} or
   *                                  {@code toElement} lies outside the bounds of the range
   */
  @Override
  public SortedSet<T> subSet(T fromElement, T toElement) {
    final int left = Collections.binarySearch(list, fromElement, comparator);
    final int right = Collections.binarySearch(list, toElement, comparator);
    if (left > right) {
      throw new IllegalArgumentException("Left border larger than right");
    }
    if (left < 0) {
      throw new IllegalArgumentException("Element not found");
    }
    return new ArraySet<>(list.subList(left, right), comparator);
  }

  /**
   * Returns a view of the portion of this set whose elements are
   * strictly less than {@code toElement}.  The returned set is
   * backed by this set, so changes in the returned set are
   * reflected in this set, and vice-versa.  The returned set
   * supports all optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException}
   * on an attempt to insert an element outside its range.
   *
   * @param toElement high endpoint (exclusive) of the returned set
   * @return a view of the portion of this set whose elements are strictly
   * less than {@code toElement}
   * @throws ClassCastException       if {@code toElement} is not compatible
   *                                  with this set's comparator (or, if the set has no comparator,
   *                                  if {@code toElement} does not implement {@link Comparable}).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code toElement} cannot be compared to elements
   *                                  currently in the set.
   * @throws NullPointerException     if {@code toElement} is null and
   *                                  this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a
   *                                  restricted range, and {@code toElement} lies outside the
   *                                  bounds of the range
   */
  @Override
  public SortedSet<T> headSet(T toElement) {
    final int right = Collections.binarySearch(list, toElement, comparator);
    if (right < 0) {
      throw new IllegalArgumentException("Element not found");
    }
    return new ArraySet<>(list.subList(0, right), comparator);
  }

  /**
   * Returns a view of the portion of this set whose elements are
   * greater than or equal to {@code fromElement}.  The returned
   * set is backed by this set, so changes in the returned set are
   * reflected in this set, and vice-versa.  The returned set
   * supports all optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException}
   * on an attempt to insert an element outside its range.
   *
   * @param fromElement low endpoint (inclusive) of the returned set
   * @return a view of the portion of this set whose elements are greater
   * than or equal to {@code fromElement}
   * @throws ClassCastException       if {@code fromElement} is not compatible
   *                                  with this set's comparator (or, if the set has no comparator,
   *                                  if {@code fromElement} does not implement {@link Comparable}).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code fromElement} cannot be compared to elements
   *                                  currently in the set.
   * @throws NullPointerException     if {@code fromElement} is null
   *                                  and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a
   *                                  restricted range, and {@code fromElement} lies outside the
   *                                  bounds of the range
   */
  @Override
  public SortedSet<T> tailSet(T fromElement) {
    final int left = Collections.binarySearch(list, fromElement, comparator);
    if (left < 0) {
      throw new IllegalArgumentException("Element not found");
    }
    return new ArraySet<>(list.subList(left, list.size()), comparator);
  }

  /**
   * Returns the first (lowest) element currently in this set.
   *
   * @return the first (lowest) element currently in this set
   * @throws NoSuchElementException if this set is empty
   */
  @Override
  public T first() {
    if (list.isEmpty()) {
      throw new NoSuchElementException("set is empty");
    }
    return list.get(0);
  }

  /**
   * Returns the last (highest) element currently in this set.
   *
   * @return the last (highest) element currently in this set
   * @throws NoSuchElementException if this set is empty
   */
  @Override
  public T last() {
    if (list.isEmpty()) {
      throw new NoSuchElementException("set is empty");
    }
    return list.get(list.size() - 1);
  }

  @Override
  public boolean contains(Object element) {
    Objects.requireNonNull(element);
    return Collections.binarySearch(list, (T) element, comparator) >= 0;
  }
}
