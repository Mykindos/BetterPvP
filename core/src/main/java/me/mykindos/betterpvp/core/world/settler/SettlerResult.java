package me.mykindos.betterpvp.core.world.settler;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** How a settler action went: done, or refused with a reason to show the player. */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlerResult {

    boolean success;
    @Nullable Component reason;
    @Nullable Settler settler;

    public static @NotNull SettlerResult done(@NotNull Settler settler) {
        return new SettlerResult(true, null, settler);
    }

    /** Refused with the message under translation key {@code key}. */
    public static @NotNull SettlerResult refused(@NotNull String key, @NotNull ComponentLike... args) {
        return new SettlerResult(false, Translations.component(key, args).color(NamedTextColor.RED), null);
    }
}
