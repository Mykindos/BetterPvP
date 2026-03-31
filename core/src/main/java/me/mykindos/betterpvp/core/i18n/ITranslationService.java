package me.mykindos.betterpvp.core.i18n;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface ITranslationService {

    void register();

    @NotNull Locale getDefaultLocale();

    @NotNull Locale getPlayerLocale(@NotNull Player player);

    @NotNull Locale normalizeLocale(@Nullable String languageTag);

    @NotNull Locale normalizeLocale(@Nullable Locale locale);

    @Nullable String getRaw(@NotNull String key, @NotNull Locale locale);

    default @Nullable String getRaw(@NotNull String key, @NotNull Player player) {
        return getRaw(key, getPlayerLocale(player));
    }

    @NotNull String translateString(@NotNull String key, @NotNull Locale locale);

    default @NotNull String translateString(@NotNull String key, @NotNull Player player) {
        return translateString(key, getPlayerLocale(player));
    }
}

