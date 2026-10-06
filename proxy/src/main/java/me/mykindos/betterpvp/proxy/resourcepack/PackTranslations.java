package me.mykindos.betterpvp.proxy.resourcepack;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Registers the pack prompt and disconnect messages with the global translator, so Velocity renders them in each
 * player's language. English is the fallback.
 */
public final class PackTranslations {

    private static final List<String> LANGUAGES = List.of("en", "ar", "de", "es", "fr", "ja", "ko", "ms", "nl", "pl", "ru", "zh");

    private PackTranslations() {
    }

    public static void register() throws IOException {
        final TranslationStore.StringBased<MessageFormat> store = TranslationStore.messageFormat(Key.key("betterpvp", "proxy"));
        store.defaultLocale(Locale.ENGLISH);
        for (String language : LANGUAGES) {
            final Locale locale = Locale.forLanguageTag(language);
            try (InputStream stream = PackTranslations.class.getResourceAsStream("/translations/proxy_" + language + ".properties")) {
                if (stream == null) {
                    throw new IOException("Missing proxy translations for " + language);
                }
                final Properties properties = new Properties();
                properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                properties.forEach((key, value) -> store.register((String) key, locale, new MessageFormat((String) value, locale)));
            }
        }
        GlobalTranslator.translator().addSource(store);
    }

}
