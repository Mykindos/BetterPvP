package me.mykindos.betterpvp.core.menu.navigation;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * One entry in a {@link NavigationMenu}: how it is shown, and what choosing it does.
 * <p>
 * Deliberately says nothing about where the player ends up or how long getting there takes. A destination that
 * teleports and one that starts a journey ending minutes later are the same thing to a menu, so the menu is not told
 * which it has. Whatever needs to know owns {@link #select(Player)}.
 */
public interface Destination {

    /**
     * @return the name shown to players in the navigator
     */
    @NotNull Component displayName();

    /**
     * @return the icon shown for this destination in a navigator menu
     */
    @NotNull ItemView icon();

    /**
     * Runs whatever choosing this destination means. Called once, after the menu has closed, on the main thread.
     * <p>
     * Refusals belong here rather than to the menu, since only the destination knows what would refuse and what to
     * say about it.
     *
     * @param traveller the player who chose it
     */
    void select(@NotNull Player traveller);
}
