package me.mykindos.betterpvp.core.world.zone;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The game mode a {@link Zone} puts players in. Attached to a zone at build time, it is what lets an area declare how
 * it is played instead of that decision living in one listener that has to know every area on the server.
 * <p>
 * Resolution takes the player, because an area can be played differently by different people: clan territory is
 * survival for its own members and a pillaging enemy, and adventure for everyone else. Returning {@code null} is an
 * abstention - the zone has no opinion for this player and the next zone down (or the caller's fallback) decides.
 */
@FunctionalInterface
public interface ZoneGameMode {

    /**
     * @param player the player standing in the zone
     * @return the game mode this zone wants them in, or {@code null} to abstain
     */
    @Nullable GameMode resolve(@NotNull Player player);

    /**
     * @param gameMode the mode everyone inside plays in
     * @return a resolver that answers the same mode for every player
     */
    static @NotNull ZoneGameMode of(@NotNull GameMode gameMode) {
        return player -> gameMode;
    }
}
