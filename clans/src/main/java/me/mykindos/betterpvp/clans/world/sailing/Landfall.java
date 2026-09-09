package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The destination of a voyage, and what happens when the crew arrives there.
 * <p>
 * Placing the crew is the destination's responsibility rather than the service's, because the two kinds of site behave
 * differently. A permanent site only has to choose which of its arrival points to use. An on-demand site does not exist
 * while the voyage is running, since its world is cloned on arrival, so its location cannot be resolved in advance and
 * resolving it early would hold an empty world open for the whole voyage.
 *
 * @see VoyageService for the voyage itself
 */
public interface Landfall {

    /** The display name, shown during the voyage and on arrival. */
    @NotNull Component displayName();

    /** The arrival schedule for voyages to this destination. */
    @NotNull VoyageTiming timing();

    /**
     * Places the crew at the destination, provisioning it first if it does not exist yet. Called once on arrival, with
     * the players still in the staging world.
     *
     * @param sailors the players still on the voyage, captain first
     * @return whether they were placed. {@code false} returns them to their origin instead.
     */
    @NotNull CompletableFuture<Boolean> setAshore(@NotNull List<Player> sailors);
}
