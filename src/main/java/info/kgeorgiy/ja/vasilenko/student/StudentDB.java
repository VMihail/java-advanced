package info.kgeorgiy.ja.vasilenko.student;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author VMihail (vmihail399@gmail.com)
 * created: 11.03.2023 00:21
 */
public class StudentDB implements StudentQuery {
  private <T> List<T> mapper(final List<Student> list, final Function<Student, T> function) {
    return list.stream().map(function).toList();
  }

  private List<Student> sort(final Collection<Student> students, final Comparator<Student> comparator) {
    return students.stream().sorted(comparator).toList();
  }

  /**
   * Returns student {@link Student#getFirstName() first names}.
   *
   * @param students
   */
  @Override
  public List<String> getFirstNames(List<Student> students) {
    return mapper(students, Student::getFirstName);
  }

  /**
   * Returns student {@link Student#getLastName() last names}.
   *
   * @param students
   */
  @Override
  public List<String> getLastNames(List<Student> students) {
    return mapper(students, Student::getLastName);
  }

  /**
   * Returns student {@link Student#getGroup() groups}.
   *
   * @param students
   */
  @Override
  public List<GroupName> getGroups(List<Student> students) {
    return mapper(students, Student::getGroup);
  }

  /**
   * Returns full student name.
   *
   * @param students
   */
  @Override
  public List<String> getFullNames(List<Student> students) {
    return mapper(students, student -> String.format("%s %s", student.getFirstName(), student.getLastName()));
  }

  /**
   * Returns distinct student {@link Student#getFirstName() first names} in lexicographic order.
   *
   * @param students
   */
  @Override
  public Set<String> getDistinctFirstNames(List<Student> students) {
    return students.stream().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Returns a {@link Student#getFirstName() first name} of the student with maximal {@link Student#getId() id}.
   *
   * @param students
   */
  @Override
  public String getMaxStudentFirstName(List<Student> students) {
    return sort(students, Comparator.comparingInt(Student::getId)).get(0).getFirstName();
  }

  /**
   * Returns students ordered by {@link Student#getId() id}.
   *
   * @param students
   */
  @Override
  public List<Student> sortStudentsById(Collection<Student> students) {
    return sort(students, Comparator.comparingInt(Student::getId));
  }

  /**
   * Returns students ordered by name.
   *
   * @param students
   */
  @Override
  public List<Student> sortStudentsByName(Collection<Student> students) {
    return sort(
     students,
     (firstStudent, secondStudent) -> {
       final String firstName = String.format("%s %s", firstStudent.getFirstName(), firstStudent.getLastName());
       final String secondName = String.format("%s %s", secondStudent.getFirstName(), secondStudent.getLastName());
       return firstName.compareTo(secondName);
     }
    );
  }

  /**
   * Returns students having specified first name. Students are ordered by name.
   *
   * @param students
   * @param name
   */
  @Override
  public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
    return students.stream().filter(student -> student.getFirstName().equals(name))
            .sorted(Comparator.comparing(Student::getFirstName)).toList();
  }

  /**
   * Returns students having specified last name. Students are ordered by name.
   *
   * @param students
   * @param name
   */
  @Override
  public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
    return students.stream().filter(student -> student.getLastName().equals(name))
            .sorted(Comparator.comparing(Student::getLastName)).toList();
  }

  /**
   * Returns students having specified groups. Students are ordered by name.
   *
   * @param students
   * @param group
   */
  @Override
  public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
    return students.stream().filter(student -> student.getGroup().equals(group))
            .sorted(Comparator.comparing(Student::getFirstName).thenComparing(Student::getLastName)).toList();
  }

  /**
   * Returns map of group's student last names mapped to minimal first name.
   *
   * @param students
   * @param group
   */
  @Override
  public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
    return students.stream().filter(student -> student.getGroup().equals(group)).
            collect(Collectors.toMap(
             Student::getFirstName,
             Student::getLastName,
             BinaryOperator.minBy(String::compareTo)
            ));
  }
}
