package info.kgeorgiy.ja.vasilenko.implementor;

import info.kgeorgiy.ja.vasilenko.implementor.tools.ImplerException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Реализация интерфейса {@link Impler}
 * @author VMihail (vmihail399@gmail.com)
 * created: 12.03.2023 12:54
 * @since 19
 * @version 19
 */
public class Implementor implements Impler {
  /**
   * Рекурсивное удаление файлов и директорий, основан на {@link SimpleFileVisitor}
   */
  private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
    /**
     * Удаление файла
     *
     * @param file текущий файл
     * @param attrs атрибуты файла
     * @return {@link FileVisitResult#CONTINUE}
     * @throws IOException если не получилось удалить файл
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    /**
     * Удаление директорию
     *
     * @param dir текущая директория
     * @param exc {@code null}, если почтовый каталог без ошибок, иначе {@link IOException}
     * @return {@link FileVisitResult#CONTINUE}
     * @throws IOException если не получилось удалить директорию
     */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  };

  /**
   * Суффикс для имени класса
   */
  private static final String IMPLEMENT_SUFFIX = "Impl";

  /**
   * проверка аргументов
   *
   * @param args аргументы метода {@link #main(String...)}
   * @return false если аргументы неверные, иначе true
   */
  private static boolean checkArgs(final String[] args) {
    if (args == null || args.length != 2 && args.length != 3 || Arrays.stream(args).anyMatch(Objects::isNull)) {
      return false;
    }
    return args.length != 3 || "-jar".equals(args[0]);
  }

  /**
   * Создается <var>.jar</var> файл. Реализует класс используя {@link #implement(Class, Path)}
   * во временном каталоге рядом с созданным файлом. Скомпилируйте его и упакуйте в файл .jar
   *
   * @param token   тип токен, для которого нужно создать реализацию.
   * @param jarFile <var>.jar</var> файл.
   * @throws ImplerException если данный класс не может быть реализован, вызывает:
   * <ul>
   * <li>Токен не является интерфейсом</li>
   * <li>Некоторые методы имеют частный класс для аргументов, исключений или возврата</li>
   * <li>IO exception</li>
   * <li>Не удалось выполнить компиляцию</li>
   * </ul>
   */
  public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
    final Path tempDir;
    try {
      final Path parent = jarFile.toAbsolutePath().getParent();
      if (parent == null) {
        throw new ImplerException("parent of jar file is null");
      }
      tempDir = Files.createTempDirectory(parent, "temp");
    } catch (final IOException e) {
      throw new ImplerException("Failed create temp directory");
    }
    implement(token, tempDir);
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new ImplerException("Compiler not found");
    }
    final String[] args = new String[]{
      "-cp",
      getClassPath(token),
      getJavaFilePath(token, tempDir).toString()
    };
    if (compiler.run(null, null, null, args) != 0) {
      throw new ImplerException("Fail compile generated code");
    }
    final Manifest manifest = new Manifest();
    final Attributes attributes = manifest.getMainAttributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    try (final JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
      final String className;
      if (token.getPackageName().equals("")) {
        className = token.getSimpleName();
      } else {
        className = token.getPackageName().replace('.', '/') + "/" + token.getSimpleName();
      }
      writer.putNextEntry(new ZipEntry(className + IMPLEMENT_SUFFIX + ".class"));
      final Path javaFile = resolvePackage(token, tempDir).resolve(token.getSimpleName() + IMPLEMENT_SUFFIX + ".class");
      Files.copy(javaFile, writer);
    } catch (final IOException e) {
      throw new ImplerException("Fail write JAR file", e);
    } finally {
      try {
        recursivelyCleanDirectory(tempDir);
      } catch (final IOException e) {
        System.err.println("Failed clean temp directory: " + e.getMessage());
      }
    }
  }

  /**
   * Строка из символов Юникода
   *
   * @param input входная строка
   * @return Строка из символов Юникода
   */
  private String convertToUnicode(final String input) {
    final StringBuilder result = new StringBuilder();
    for (char ch : input.toCharArray()) {
      if (ch >= 128) {
        result.append(String.format("\\u%04X", (int) ch));
      } else {
        result.append(ch);
      }
    }
    return result.toString();
  }

  /**
   * Генерация реализации по токену и добавление ее в корень
   *
   * @param token тип токена, для которого нужно создать реализацию.
   * @param root  коневая директория.
   * @throws ImplerException если данный класс не может быть реализован, вызывает:
   * <ul>
   * <li>Токен не является интерфейсом</li>
   * <li>Некоторые методы имеют частный класс для аргументов, исключений или возврата</li>
   * <li>IO exception</li>
   * <li>Не удалось выполнить компиляцию</li>
   * </ul>
   */
  @Override
  public void implement(final Class<?> token, final Path root) throws ImplerException {
    try (final BufferedWriter writer = Files.newBufferedWriter(createJavaFilePath(token, root))) {
      final CodeGenerator codeGenerator = new CodeGenerator(token, IMPLEMENT_SUFFIX);
      final String clazz = codeGenerator.generateImplementation();
      writer.write(convertToUnicode(clazz));
    } catch (final IOException e) {
      throw new ImplerException("Fail write generated implementation", e);
    }
  }

  /**
   * Путь класса к файлу .jar файлу.
   *
   * @param token токен для получения пути к классу
   * @return class path для .jar файла
   * @throws ImplerException если не удалось получить путь к классу для токена
   */
  private static String getClassPath(final Class<?> token) throws ImplerException {
    try {
      return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    } catch (final URISyntaxException e) {
      throw new ImplerException("Failed get class path for token", e);
    }
  }

  /**
   * Путь пакета относительно корня
   *
   * @param token тип токена, для которого нужно создать реализацию.
   * @param root  директория
   * @return root + packagePath
   */
  private static Path resolvePackage(final Class<?> token, final Path root) {
    return root.resolve(token.getPackageName().replace('.', File.separatorChar));
  }

  /**
   * Реализация файла Java относительно корня
   *
   * @param token тип токена, для которого нужно создать реализацию.
   * @param root  директория
   * @return root + package + name java file
   */
  private static Path getJavaFilePath(final Class<?> token, final Path root) {
    return resolvePackage(token, root).resolve(token.getSimpleName() + IMPLEMENT_SUFFIX + ".java");
  }

  /**
   * Создать путь к реализации файла Java относительно корня
   *
   * @param token тип токена, для которого нужно создать реализацию.
   * @param root  директория
   * @return root + package + name java file
   */
  private static Path createJavaFilePath(final Class<?> token, final Path root) {
    final Path javaFilePath = getJavaFilePath(token, root);
    try {
      Files.createDirectories(javaFilePath.getParent());
    } catch (final IOException e) {
      System.err.println("Fail create directories");
    }
    return javaFilePath;
  }

  /**
   * Рекурсивно удаляет файлы и каталоги в корне
   *
   * @param root каталог для рекурсивного удаления
   * @throws IOException если ошибка рекурсивно удалить
   */
  private static void recursivelyCleanDirectory(final Path root) throws IOException {
    if (Files.exists(root)) {
      Files.walkFileTree(root, DELETE_VISITOR);
    }
  }

  /**
   * Генерация реализации
   * <ul>
   *     <li> для использования {@link #implement(Class, Path)}
   *     укажите первым аргументом укажите имя класса, вторым выходную директорию
   *     </li>
   *     <li> для использования {@link #implementJar(Class, Path)}
   *     укажите первым аргументом -jar, вторым имя класса, путь до jar файла
   *
   *     </li>
   * </ul>
   *
   * @param args аргументы для генерации реализации
   */
  public static void main(final String ...args) {
    if (!checkArgs(args)) {
      System.err.printf("Usage: java %s [-jar] <Class name> <Target path>", Implementor.class.getSimpleName());
      System.err.println();
      return;
    }
    final Implementor implementor = new Implementor();
    try {
      if (args.length == 3) {
        implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
      } else {
        implementor.implement(Class.forName(args[0]), Path.of(args[1]));
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Class not found");
    } catch (ImplerException e) {
      System.err.println("Fail implement this class");
    }
  }
}
