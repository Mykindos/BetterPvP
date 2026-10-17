package me.mykindos.betterpvp.core.command.brigadier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Command framework text in every locale")
class FrameworkTranslationsTest {

    private static final List<String> LOCALES = List.of("ar", "de", "en", "es", "fr", "ja", "ko", "ms", "nl", "pl", "ru", "zh");
    private static final List<String> PREFIXES = List.of("core.command.error.", "core.command.requirement.");

    private static Properties load(String locale) throws IOException {
        final String path = "/translations/core_" + locale + ".properties";
        try (InputStream stream = FrameworkTranslationsTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            final Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        }
    }

    private static Set<String> frameworkKeys(Properties properties) {
        final Set<String> keys = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (PREFIXES.stream().anyMatch(key::startsWith)) {
                keys.add(key);
            }
        }
        return keys;
    }

    @Test
    @DisplayName("AC27 every framework error and requirement key exists in all 12 locales")
    void ac27_frameworkKeysInEveryLocale() throws IOException {
        final Set<String> english = frameworkKeys(load("en"));
        assertTrue(english.containsAll(Set.of(
                "core.command.error.insufficient_permission",
                "core.command.error.player_only",
                "core.command.error.on_cooldown",
                "core.command.error.unknown_player",
                "core.command.error.unknown_offline_player",
                "core.command.error.invalid_player_name",
                "core.command.error.unknown_effect",
                "core.command.error.unknown_item",
                "core.command.error.unknown_uuid_item",
                "core.command.error.invalid_boolean",
                "core.command.error.invalid_duration",
                "core.command.requirement.can_run",
                "core.command.requirement.cannot_run")), english.toString());

        for (String locale : LOCALES) {
            assertEquals(english, frameworkKeys(load(locale)), locale);
        }
    }
}
