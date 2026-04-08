package me.mykindos.betterpvp.core.i18n;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class TranslationRegistry {

    private static volatile ITranslationService translationService = new NoopTranslationService();

    private TranslationRegistry() {
    }

    public static void set(@NotNull ITranslationService service) {
        translationService = service;
    }

    public static @NotNull ITranslationService get() {
        return translationService;
    }

    private static final class NoopTranslationService implements ITranslationService {

        private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

        @Override
        public void register() {
        }

        @Override
        public @NotNull Locale getDefaultLocale() {
            return DEFAULT_LOCALE;
        }

        @Override
        public @NotNull Locale getPlayerLocale(@NotNull Player player) {
            return normalizeLocale(player.locale());
        }

        @Override
        public @NotNull Locale normalizeLocale(@Nullable String languageTag) {
            if (languageTag == null || languageTag.isBlank()) {
                return DEFAULT_LOCALE;
            }

            return normalizeLocale(Locale.forLanguageTag(languageTag.replace('_', '-')));
        }

        @Override
        public @NotNull Locale normalizeLocale(@Nullable Locale locale) {
            if (locale == null) {
                return DEFAULT_LOCALE;
            }

            String language = locale.getLanguage();
            if (language == null || language.isBlank()) {
                return DEFAULT_LOCALE;
            }

            return Locale.forLanguageTag(language.toLowerCase(Locale.ROOT));
        }

        @Override
        public @Nullable String getRaw(@NotNull String key, @NotNull Locale locale) {
            return null;
        }

        @Override
        public @NotNull String translateString(@NotNull String key, @NotNull Locale locale) {
            return key;
        }
    }
}

