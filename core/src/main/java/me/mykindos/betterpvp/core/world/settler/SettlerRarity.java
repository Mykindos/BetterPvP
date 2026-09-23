package me.mykindos.betterpvp.core.world.settler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** How rare a settler is, from most to least common. */
@Getter
@AllArgsConstructor
public enum SettlerRarity {
    COMMON(NamedTextColor.WHITE),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.BLUE),
    LEGENDARY(NamedTextColor.GOLD);

    /** What its settlers' names are shown in. */
    private final TextColor color;

    public boolean isAtLeast(@NotNull SettlerRarity other) {
        return compareTo(other) >= 0;
    }

    public @NotNull Component displayName() {
        return Translations.component("core.settler.rarity." + name().toLowerCase(Locale.ROOT)).color(color);
    }
}
