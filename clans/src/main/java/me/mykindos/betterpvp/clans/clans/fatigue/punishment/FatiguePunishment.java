package me.mykindos.betterpvp.clans.clans.fatigue.punishment;

import me.mykindos.betterpvp.clans.clans.fatigue.FatigueTier;
import org.bukkit.entity.Player;

/**
 * A consequence applied to a fatigued player <i>when they are released from the
 * respawn hold</i> (or immediately on respawn if their tier requires no hold).
 * <p>
 * Discovered via a Guice {@code Multibinder}; {@code RespawnHoldService} runs
 * every punishment whose {@link #appliesTo(FatigueTier)} accepts the player's
 * tier. Adding a punishment is one binding line — no orchestration code changes.
 */
public interface FatiguePunishment {

    /**
     * @return whether this punishment should fire for the given tier.
     */
    boolean appliesTo(FatigueTier tier);

    /**
     * Apply the consequence. Called on the main thread, after the player has
     * been returned to the world.
     *
     * @param player the recovering player
     * @param tier   the tier that triggered the punishment (for scaling)
     */
    void apply(Player player, FatigueTier tier);
}
