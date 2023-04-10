package info.kgeorgiy.ja.vasilenko.student;

public class Main {
  public static void main(String[] args) {
    GroupName a = new GroupName("M32071");
    GroupName b = new GroupName("M32071");
    System.out.println(a.name());
    System.out.println(a.equals(b) + " " + (a.hashCode() == b.hashCode()));
    System.out.println(a);
  }
}
