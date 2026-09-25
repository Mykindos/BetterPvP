package me.mykindos.betterpvp.core.utilities.model.tag;

import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import org.jetbrains.annotations.NotNull;

public class CoinsTag implements Modifying {

    /** {@code amount} followed by the coin icon, in the coin colour unless {@code amount} has its own. */
    public static @NotNull Component of(@NotNull Component amount) {
        return Component.text()
                .append(amount.colorIfAbsent(TextColor.color(255, 183, 0)))
                .append(Component.text("ꓯ", NamedTextColor.WHITE).font(Resources.Font.NEXO))
                .build();
    }

    /** {@code amount} coins, formatted, with the coin icon. */
    public static @NotNull Component of(long amount) {
        return of(Component.text(String.format("%,d", amount)));
    }

    @Override
    public Component apply(@NotNull Component current, int depth) {
        if (depth != 0) return Component.empty();
        return of(current);
    }
}
