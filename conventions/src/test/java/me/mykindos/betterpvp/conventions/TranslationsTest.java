package me.mykindos.betterpvp.conventions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Player-facing text is translated into every locale. Each module's translation folder holds one
 * {@code <module>_<locale>.properties} file per locale, all with the same keys, and every literal key the code
 * passes to {@code Translations} exists in some module.
 */
class TranslationsTest {

    private static final Set<String> LOCALES = Set.of(
            "ar", "de", "en", "es", "fr", "ja", "ko", "ms", "nl", "pl", "ru", "zh");
    private static final Pattern KEY_USE = Pattern.compile(
            "\\bTranslations\\.(?:component|componentLines|rawComponentLines)\\(\\s*\"([^\"]+)\"");

    @Test
    void everyModuleShipsEveryLocale() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path dir : Codebase.translationDirs()) {
            Set<String> locales = bundles(dir).keySet();
            if (!locales.equals(LOCALES)) {
                Set<String> missing = new TreeSet<>(LOCALES);
                missing.removeAll(locales);
                Set<String> extra = new TreeSet<>(locales);
                extra.removeAll(LOCALES);
                failures.add(Codebase.relative(dir) + ": missing " + missing + ", unexpected " + extra);
            }
        }
        assertEquals(List.of(), failures, "Every translation folder needs exactly the 12 locale files");
    }

    @Test
    void everyLocaleHasTheSameKeys() throws IOException {
        List<String> failures = new ArrayList<>();
        for (Path dir : Codebase.translationDirs()) {
            Map<String, Set<String>> bundles = bundles(dir);
            Set<String> all = new TreeSet<>();
            bundles.values().forEach(all::addAll);
            bundles.forEach((locale, keys) -> {
                Set<String> missing = new TreeSet<>(all);
                missing.removeAll(keys);
                if (!missing.isEmpty()) {
                    failures.add(Codebase.relative(dir) + " [" + locale + "] is missing " + missing);
                }
            });
        }
        if (!failures.isEmpty()) {
            fail("Translation keys must exist in all 12 locale files:\n" + String.join("\n", failures));
        }
    }

    @Test
    void literalKeysInCodeExist() throws IOException {
        Set<String> known = new HashSet<>();
        for (Path dir : Codebase.translationDirs()) {
            bundles(dir).values().forEach(known::addAll);
        }
        List<String> failures = new ArrayList<>();
        for (Path file : Codebase.javaSources()) {
            JavaText text = new JavaText(Codebase.read(file));
            Matcher matcher = KEY_USE.matcher(text.getSource());
            while (matcher.find()) {
                String key = matcher.group(1);
                if (text.inCode(matcher.start()) && !isDynamic(key) && !isKnown(key, known)) {
                    failures.add(Codebase.relative(file) + ":" + text.lineOf(matcher.start()) + " " + key);
                }
            }
        }
        if (!failures.isEmpty()) {
            fail("Translation keys used in code but missing from every locale file:\n" + String.join("\n", failures));
        }
    }

    /**
     * Keys ending in a dot are prefixes the code completes at runtime.
     */
    private static boolean isDynamic(String key) {
        return key.endsWith(".");
    }

    /**
     * Multi-line text is stored as numbered child keys, so a key also counts when it is the parent of others.
     */
    private static boolean isKnown(String key, Set<String> known) {
        if (known.contains(key)) {
            return true;
        }
        String prefix = key + ".";
        return known.stream().anyMatch(candidate -> candidate.startsWith(prefix));
    }

    private static Map<String, Set<String>> bundles(Path dir) throws IOException {
        Map<String, Set<String>> bundles = new TreeMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".properties")).toList()) {
                String name = file.getFileName().toString();
                String locale = name.substring(name.lastIndexOf('_') + 1, name.length() - ".properties".length());
                Properties properties = new Properties();
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
                bundles.put(locale, properties.stringPropertyNames());
            }
        }
        return bundles;
    }
}
