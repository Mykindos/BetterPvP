package me.mykindos.betterpvp.game.framework.model.chat;

import me.mykindos.betterpvp.game.framework.AbstractGame;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a color for a player
 */
@FunctionalInterface
public interface PlayerColorProvider  {

    /**
     * Generates a chat color for a player
     *
     * @param player The player to generate a color for
     * @param game The current game
     * @return The spawn point for the player
     */
    @NotNull TextColor getColor(Player player, AbstractGame<?, ?> game);

}
