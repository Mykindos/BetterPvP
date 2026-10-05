package me.mykindos.betterpvp.conventions;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The checked modules' compiled classes, Java sources and translation folders, located through the system properties
 * the Gradle test task sets.
 */
final class Codebase {

    static final Path ROOT = Path.of(System.getProperty("conventions.root"));
    static final List<String> MODULES = List.of(System.getProperty("conventions.modules").split(","));
    static final boolean UPDATE_BASELINE = Boolean.getBoolean("conventions.updateBaseline");

    private static JavaClasses classes;

    private Codebase() {
    }

    static synchronized JavaClasses classes() {
        if (classes == null) {
            List<Path> dirs = MODULES.stream()
                    .map(module -> ROOT.resolve(module).resolve("build/classes/java/main"))
                    .filter(Files::isDirectory)
                    .toList();
            classes = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .withImportOption(location -> !location.contains("/database/jooq/"))
                    .importPaths(dirs);
        }
        return classes;
    }

    /**
     * Every handwritten Java source file in the checked modules. Generated jOOQ code is left out.
     */
    static List<Path> javaSources() {
        return MODULES.stream()
                .map(module -> ROOT.resolve(module).resolve("src/main/java"))
                .filter(Files::isDirectory)
                .flatMap(Codebase::walk)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !relative(path).contains("/database/jooq/"))
                .sorted()
                .toList();
    }

    static List<Path> translationDirs() {
        return MODULES.stream()
                .map(module -> ROOT.resolve(module).resolve("src/main/resources/translations"))
                .filter(Files::isDirectory)
                .toList();
    }

    static String relative(Path path) {
        return ROOT.relativize(path).toString().replace('\\', '/');
    }

    static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<Path> walk(Path dir) {
        try {
            return Files.walk(dir).toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
