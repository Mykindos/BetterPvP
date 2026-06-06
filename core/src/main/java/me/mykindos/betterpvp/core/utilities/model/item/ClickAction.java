package me.mykindos.betterpvp.core.utilities.model.item;

import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a click action
 */
public interface ClickAction {

    /**
     * @return The name of this action to display in UIs
     */
    @NotNull Component getComponent();

    /**
     * @param clickType The {@link ClickType} to check
     * @return Whether this action accepts the given {@link ClickType}
     */
    boolean accepts(@NotNull ClickType clickType);

    default Component to(Component action) {
        return Component.empty()
                .append(Component.text("\uE0FF", NamedTextColor.WHITE).font(Key.key("betterpvp", "input")))
                .append(Component.translatable("space.-1").font(Resources.Font.SPACE))
                .append(Component.text("\uE100", NamedTextColor.WHITE).font(Key.key("betterpvp", "input")))
                .append(Component.translatable("space.2").font(Resources.Font.SPACE))
                .append(Component.text("+", NamedTextColor.GRAY))
                .append(Component.translatable("space.2").font(Resources.Font.SPACE))
                .append(Component.text("\uE04D", NamedTextColor.WHITE).font(Key.key("betterpvp", "input")))
//                .append(getComponent())
                .appendSpace()
                .append(Component.text("to", NamedTextColor.WHITE))
                .appendSpace()
                .append(action.applyFallbackStyle(NamedTextColor.YELLOW));
    }

}
