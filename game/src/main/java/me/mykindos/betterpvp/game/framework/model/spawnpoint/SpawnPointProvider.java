package me.mykindos.betterpvp.game.framework.model.spawnpoint;

import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Generates a spawn point for a player
 */
@FunctionalInterface
public interface SpawnPointProvider {

    /**
     * Generates a spawn point for a player. The player, at this point, is guaranteed to be a participant in the game
     * and alive.
     *
     * @param player The player to generate a spawn point for
     * @param map The current map
     * @param game The current game
     * @return The spawn point for the player
     */
    @NotNull Location getSpawnPoint(Player player, MappedWorld map, AbstractGame<?, ?> game);

}
