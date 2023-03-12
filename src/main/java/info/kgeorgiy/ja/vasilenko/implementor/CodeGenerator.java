package info.kgeorgiy.ja.vasilenko.implementor;

import info.kgeorgiy.ja.vasilenko.implementor.tools.ImplerException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Генератор реализации по токену класса
 * @author VMihail (vmihail399@gmail.com)
 * @since 19
 * @version 19
 */
public record CodeGenerator(Class<?> token, StringBuilder sb, String implementSuffix) {
  /**
   * пробел для сгенерированного кода.
   */
  private static final String SPACE = " ";

  /**
   * табуляция для сгенерированного кода.
   */
  private static final String TAB = "\t";

  /**
   * новая строка для сгенерированного кода.
   */
  private static final String NEW_LINE = "\n";

  /**
   * , для сгенерированного кода.
   */
  private static final String COMMA = ",";

  /**
   * ; для сгенерированного кода.
   */
  private static final String SEMICOLON = ";";

  /**
   * Фигурная открывающаяся скобка для сгенерированного кода.
   */
  private static final String CURLY_OPEN_BRACKET = "{";

  /**
   * Фигурная закрывающая скобка для сгенерированного кода.
   */
  private static final String CURLY_CLOSE_BRACKET = "}";

  /**
   * {@code null} является значением по умолчанию для некоторых методов.
   */
  private static final String NULL = "null";

  /**
   * Число ноль является значением по умолчанию для некоторых методов.
   */
  private static final String ZERO = "0";

  /**
   * Логическое значение false является значением по умолчанию для некоторых методов
   */
  private static final String FALSE = "false";

  /**
   * Пакет - используется для сгенерированного кода.
   */
  private static final String PACKAGE = "package";

  /**
   * Возврат - для сгенерированного кода.
   */
  private static final String RETURN = "return";

  /**
   * Круглая открытая скобка для сгенерированного кода.
   */
  private static final String ROUND_OPEN_BRACKET = "(";

  /**
   * Круглая закрывающая скобка для сгенерированного кода.
   */
  private static final String ROUND_CLOSE_BRACKET = ")";

  /**
   * Предназначен для генерирования кода исключения.
   */
  private static final String THROWS = "throws";

  /**
   * Пустая строка для сгенерированного кода
   */
  private static final String EMPTY_STRING = "";

  /**
   * Стандартный конструктор.
   *
   * @param token      {@link #token}
   * @param implementSuffix {@link #implementSuffix}
   * @throws ImplerException если токен приватный не является интерфейсом
   */
  public CodeGenerator(final Class<?> token, final String implementSuffix) throws ImplerException {
    this(token, new StringBuilder(), implementSuffix);
    if (!token.isInterface()) {
      throw new ImplerException("only interfaces are supported");
    } else if (Modifier.isPrivate(token.getModifiers())) {
      throw new ImplerException("Can't implement private interface");
    }
  }


  /**
   * Генерирование реализации. Методы имеют тело со значениями по умолчанию
   *
   * @return implementation
   * @throws ImplerException если метод имеет приватный класс
   */
  public String generateImplementation() throws ImplerException {
    generatePackage();
    generateTitle();
    sb.append(SPACE).append(CURLY_OPEN_BRACKET);
    generateNewLine();
    generateMethods();
    sb.append(CURLY_CLOSE_BRACKET);
    return sb.toString();
  }

  /**
   * Генерация пакета, если он существует
   */
  private void generatePackage() {
    if (!token.getPackageName().equals(EMPTY_STRING)) {
      sb.append(PACKAGE).append(SPACE);
      sb.append(token.getPackageName());
      sb.append(SEMICOLON);
      generateNewLine();
      generateNewLine();
    }
  }

  /**
   *
   * Генерация титула. Имя с {@link #implementSuffix} и токеном реализации.
   * Титул без открывающей фигурной скобки в конце
   */
  private void generateTitle() {
    sb.append(String.format(
      "public class %s implements %s", token.getSimpleName() + implementSuffix, token.getCanonicalName()
    ));
  }

  /**
   * Генерация метода. Методы имеют значения по умолчанию
   *
   * @throws ImplerException если метод имеет приватный класс
   */
  private void generateMethods() throws ImplerException {
    for (Method method : token.getMethods()) {
      generateNewLine();
      sb.append(TAB);
      sb.append(getSignature(method));
      sb.append(SPACE);
      generateMethodBody(method.getReturnType());
      generateNewLine();
    }
  }

  /**
   * Генерация сигнатуры. Без открывающей фигурной скобки на конце
   *
   * @param method метод который нужно реализовать
   * @return реализованный метод
   * @throws ImplerException если метод имеет приватный класс
   */
  private static String getSignature(final Method method) throws ImplerException {
    checkNonPrivate(Stream.of(
      method.getReturnType()),
      "Return type",
      method
    );
    checkNonPrivate(Stream.of(
      method.getParameters()).map(Parameter::getType),
      "Argument",
      method
    );
    checkNonPrivate(Stream.of(
      method.getExceptionTypes()),
      "Exception",
      method
    );
    final String modifications = Modifier.toString(
      method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT) + SPACE
      + method.getReturnType().getCanonicalName() + SPACE
      + method.getName();
    final String arguments = Arrays.stream(method.getParameters())
      .map(parameter -> parameter.getType().getCanonicalName() + SPACE + parameter.getName())
      .collect(Collectors.joining(COMMA + SPACE, ROUND_OPEN_BRACKET, ROUND_CLOSE_BRACKET));
    String exceptions = EMPTY_STRING;
    if (method.getExceptionTypes().length != 0) {
      exceptions = Arrays.stream(method.getExceptionTypes())
        .map(Class::getCanonicalName)
        .collect(Collectors.joining(COMMA + SPACE, SPACE + THROWS + SPACE, EMPTY_STRING));
    }
    return modifications + arguments + exceptions;
  }

  /**
   * Если есть что-то приватное, то бросаем {@link ImplerException}
   *
   * @param types Тип
   * @param nameType Имя типа
   * @param method Метод содежащий эти типы
   * @throws ImplerException если нашлось что-то приватное
   */
  private static void checkNonPrivate(final Stream<Class<?>> types, final String nameType, final Method method)
    throws ImplerException
  {
    for (Class<?> type : types.toList()) {
      if (Modifier.isPrivate(type.getModifiers())) {
        throw new ImplerException(nameType + SPACE + type.getName() + " is private in method " + method.getName());
      }
    }
  }

  /**
   * Генерация тела метода со значением по умолчанию
   *
   * @param returnValue возвращаемое значение метода
   */
  private void generateMethodBody(final Class<?> returnValue) {
    sb.append(CURLY_OPEN_BRACKET);
    generateNewLine();
    sb.append(TAB).append(TAB);
    generateReturnStatement(returnValue);
    generateNewLine();
    sb.append(TAB).append(CURLY_CLOSE_BRACKET);
  }

  /**
   * Создать оператор возврата со значением по умолчанию
   *
   * @param returnValue returned value on method
   */
  private void generateReturnStatement(final Class<?> returnValue) {
    if (returnValue == void.class) {
      return;
    }
    sb.append(RETURN).append(SPACE);
    if (returnValue == boolean.class) {
      sb.append(FALSE);
    } else if (returnValue.isPrimitive()) {
      sb.append(ZERO);
    } else {
      sb.append(NULL);
    }
    sb.append(SEMICOLON);
  }


  /**
   * Генерация новой линии используя {@link System#lineSeparator()}
   */
  private void generateNewLine() {
    sb.append(System.lineSeparator());
  }
}
