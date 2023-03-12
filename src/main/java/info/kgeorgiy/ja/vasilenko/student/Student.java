package info.kgeorgiy.ja.vasilenko.student;

/**
 * @author VMihail (vmihail399@gmail.com)
 * created: 11.03.2023 00:23
 */
public class Student {
  private static int nextId;

  private final int id;
  private final String firstName;
  private final String lastName;
  private final GroupName groupName;

  public Student(String firstName, String lastName, GroupName groupName) {
    this.id = ++nextId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.groupName = groupName;
  }

  public int getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public GroupName getGroup() {
    return groupName;
  }
}
