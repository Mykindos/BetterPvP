package me.mykindos.betterpvp.core.utilities.model.item;

import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a click action
 */
public interface ClickAction {

    /**
     * @return The name of this action to display in UIs
     */
    @NotNull String getName();

    /**
     * @param clickType The {@link ClickType} to check
     * @return Whether this action accepts the given {@link ClickType}
     */
    boolean accepts(@NotNull ClickType clickType);

}
