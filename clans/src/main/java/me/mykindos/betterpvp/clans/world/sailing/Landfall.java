package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The far side of a crossing: what a crew is sailing towards, and what happens when they sight it.
 * <p>
 * Putting people ashore is the destination's own business rather than the service's, because the two kinds of place a
 * ship can reach do it differently. A world that is always there only has to pick which of its docks the crew lands at.
 * A discovery island is not there at all while they are sailing — it is cloned at the moment they arrive — so its shore
 * cannot be resolved in advance, and asking for it early would leave a world standing empty for the whole voyage.
 *
 * @see VoyageService for the crossing itself
 */
public interface Landfall {

    /** The name of the place, shown while sailing and on arrival. */
    @NotNull Component displayName();

    /** How long the crossing to it takes. */
    @NotNull VoyageTiming timing();

    /**
     * Puts the crew ashore, creating the place they have been sailing to if it does not exist until they get there.
     * Called once, when land is sighted, with the sailors still standing on their stretch of ocean.
     *
     * @param sailors everyone still aboard, the captain first
     * @return whether they landed; {@code false} turns the ship around
     */
    @NotNull CompletableFuture<Boolean> setAshore(@NotNull List<Player> sailors);
}
