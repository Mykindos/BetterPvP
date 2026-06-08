package me.mykindos.betterpvp.core.locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BetterPvPTranslator implements Translator {

    private final List<BundleRegistration> bundles = new CopyOnWriteArrayList<>();

    @Override
    public Key name() {
        return Key.key("betterpvp", "translations");
    }

    public void registerBundle(ClassLoader classLoader, String baseName) {
        bundles.add(new BundleRegistration(classLoader, baseName));
    }

    @Override
    public @Nullable MessageFormat translate(@NonNull String key, @NonNull Locale locale) {
        for (BundleRegistration registration : bundles) {
            ResourceBundle bundle = registration.bundle(locale);
            if (bundle != null && bundle.containsKey(key)) {
                return new MessageFormat(bundle.getString(key), locale);
            }
        }

        return null;
    }

    public @Nullable String getString(@NonNull String key, @NonNull Locale locale) {
        for (BundleRegistration registration : bundles) {
            ResourceBundle bundle = registration.bundle(locale);
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return null;
    }

    public boolean hasTranslation(@NonNull String key) {
        for (BundleRegistration registration : bundles) {
            if (registration.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getRegisteredBaseNames() {
        return bundles.stream().map(BundleRegistration::baseName).toList();
    }

    private record BundleRegistration(ClassLoader classLoader, String baseName) {

        private boolean containsKey(String key) {
            for (Locale candidate : candidates(Locale.ENGLISH)) {
                try {
                    ResourceBundle bundle = ResourceBundle.getBundle(baseName, candidate, classLoader, UTF8ResourceBundleControl.INSTANCE);
                    if (bundle.containsKey(key)) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // Try the next candidate/fallback.
                }
            }

            return false;
        }

        private @Nullable ResourceBundle bundle(Locale locale) {
            for (Locale candidate : candidates(locale)) {
                try {
                    return ResourceBundle.getBundle(baseName, candidate, classLoader, UTF8ResourceBundleControl.INSTANCE);
                } catch (Exception ignored) {
                    // Try the next candidate/fallback.
                }
            }

            return null;
        }

        private List<Locale> candidates(Locale locale) {
            List<Locale> candidates = new ArrayList<>();
            candidates.add(locale);

            if (!locale.getLanguage().isBlank()) {
                candidates.add(Locale.forLanguageTag(locale.getLanguage()));
            }

            if (!Locale.ENGLISH.equals(locale)) {
                candidates.add(Locale.ENGLISH);
            }

            if (!Locale.ROOT.equals(locale)) {
                candidates.add(Locale.ROOT);
            }

            return candidates.stream().distinct().toList();
        }
    }
}