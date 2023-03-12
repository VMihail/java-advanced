package info.javaAdvanced.lab3;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VMihail (vmihail399@gmail.com)
 * created: 11.03.2023 00:12
 */
public class StudentDB implements StudentQuery {
  @Override
  public List<String> getFirstNames(List<Student> students) {
    return students.stream().map(Student::getFirstName).toList();
  }

  @Override
  public List<String> getLastNames(List<Student> students) {
    return students.stream().map(Student::getLastName).toList();
  }

  @Override
  public List<GroupName> getGroups(List<Student> students) {
    return students.stream().map(Student::getGroup).toList();
  }

  @Override
  public List<String> getFullNames(List<Student> students) {
    return students.stream().map(student -> student.getFirstName() + " " + student.getLastName()).toList();
  }

  @Override
  public Set<String> getDistinctFirstNames(List<Student> students) {
    return null;
  }

  @Override
  public String getMaxStudentFirstName(List<Student> students) {
    return null;
  }

  @Override
  public List<Student> sortStudentsById(Collection<Student> students) {
    return null;
  }

  @Override
  public List<Student> sortStudentsByName(Collection<Student> students) {
    return null;
  }

  @Override
  public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
    return null;
  }

  @Override
  public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
    return null;
  }

  @Override
  public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
    return null;
  }

  @Override
  public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
    return null;
  }
}
