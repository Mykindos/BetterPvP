package me.mykindos.betterpvp.core.client.stats.display;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatFormatterUtility {

    public static Component formatStat(String title, Object value) {
        return formatStat(title, String.valueOf(value));
    }

    public static Component formatStat(String title, String value) {
        return Component.text(title, NamedTextColor.YELLOW)
                .append(Component.text(":", NamedTextColor.GRAY))
                .appendSpace()
                .append(Component.text(value, NamedTextColor.WHITE));
    }
}
