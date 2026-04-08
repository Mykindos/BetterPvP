package me.mykindos.betterpvp.core.i18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@CustomLog
public class PlatformTranslator implements Translator, ITranslationService {

    private static final String DEFAULT_BUNDLE = "i18n/messages";

    private final Core plugin;
    private final ConcurrentMap<Locale, ResourceBundle> bundleCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Locale, ResourceBundle> platformBundleCache = new ConcurrentHashMap<>();
    private final Set<Locale> validLocales = new LinkedHashSet<>();
    private Locale defaultLocale = Locale.ENGLISH;
    private boolean strictKeys;
    private boolean registered;

    @Inject
    public PlatformTranslator(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized void register() {
        loadConfiguration();
        if (!registered) {
            GlobalTranslator.translator().addSource(this);
            registered = true;
        }
        TranslationRegistry.set(this);
    }

    @Override
    public @NotNull Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public @NotNull Locale getPlayerLocale(@NotNull Player player) {
        return normalizeLocale(player.locale());
    }

    @Override
    public @NotNull Locale normalizeLocale(@Nullable String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return defaultLocale;
        }

        return normalizeLocale(Locale.forLanguageTag(languageTag.replace('_', '-')));
    }

    @Override
    public @NotNull Locale normalizeLocale(@Nullable Locale locale) {
        if (locale == null) {
            return defaultLocale;
        }

        String language = locale.getLanguage();
        if (language == null || language.isBlank()) {
            return defaultLocale;
        }

        return Locale.forLanguageTag(language.toLowerCase(Locale.ROOT));
    }

    @Override
    public @Nullable String getRaw(@NotNull String key, @NotNull Locale locale) {
        Locale normalized = normalizeLocale(locale);
        ResourceBundle platformBundle = platformBundleCache.get(normalized);
        if (platformBundle != null && platformBundle.containsKey(key)) {
            return platformBundle.getString(key);
        }

        ResourceBundle bundle = bundleCache.get(normalized);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }

        if (!normalized.equals(defaultLocale)) {
            ResourceBundle fallbackBundle = platformBundleCache.get(defaultLocale);
            if (fallbackBundle != null && fallbackBundle.containsKey(key)) {
                return fallbackBundle.getString(key);
            }

            fallbackBundle = bundleCache.get(defaultLocale);
            if (fallbackBundle != null && fallbackBundle.containsKey(key)) {
                return fallbackBundle.getString(key);
            }
        }

        return null;
    }

    @Override
    public @NotNull String translateString(@NotNull String key, @NotNull Locale locale) {
        String raw = getRaw(key, locale);
        if (raw == null) {
            if (strictKeys) {
                throw new IllegalArgumentException("Invalid translation key: " + key);
            }
            return key;
        }

        return raw;
    }

    @Override
    public @NotNull Key name() {
        return Key.key("betterpvp", "default");
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        String raw = getRaw(key, locale);
        if (raw == null) {
            return null;
        }

        return new MessageFormat(raw, normalizeLocale(locale));
    }

    private void loadConfiguration() {
        bundleCache.clear();
        platformBundleCache.clear();
        validLocales.clear();

        defaultLocale = normalizeLocale(plugin.getConfig().getOrSaveString("core.i18n.default-locale", "en"));
        strictKeys = plugin.getConfig().getOrSaveBoolean("core.i18n.strict-keys", false);

        @SuppressWarnings("unchecked")
        List<String> localeTags = plugin.getConfig().getOrSaveObject("core.i18n.locales", List.of("en"), List.class);
        if (localeTags.isEmpty()) {
            localeTags = List.of(defaultLocale.toLanguageTag());
        }

        for (String localeTag : localeTags) {
            validLocales.add(normalizeLocale(localeTag));
        }

        // Ensure fallback locale is always present.
        if (!validLocales.contains(defaultLocale)) {
            validLocales.add(defaultLocale);
        }

        loadBundle(DEFAULT_BUNDLE, bundleCache);

        @SuppressWarnings("unchecked")
        List<String> overrideBundles = plugin.getConfig().getOrSaveObject("core.i18n.platform-bundles", List.of(), List.class);
        for (String overrideBundle : overrideBundles) {
            addPlatformBundle(overrideBundle);
        }
    }

    public void addPlatformBundle(@NotNull String bundleBaseName) {
        loadBundle(bundleBaseName, platformBundleCache);
    }

    private void loadBundle(@NotNull String bundleBaseName, @NotNull ConcurrentMap<Locale, ResourceBundle> target) {
        for (Locale locale : validLocales) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(
                        bundleBaseName,
                        locale,
                        UTF8ResourceBundleControl.utf8ResourceBundleControl()
                );
                target.put(normalizeLocale(locale), bundle);
            } catch (MissingResourceException ex) {
                log.warn("Failed to load i18n bundle {} for locale {}", bundleBaseName, locale).submit();
            }
        }
    }
}


