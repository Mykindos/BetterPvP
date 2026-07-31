package me.mykindos.betterpvp.clans.world.travel;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Somewhere a player can be sent by {@link TravelService} — a static island, a discovery island instance, or (later) a
 * place that lives on another server entirely.
 * <p>
 * A destination owns exactly one thing: getting the traveller to itself, via {@link #receive(Player)}. Everything
 * around that — the guards, the freeze, the cues, recording where the player came from — belongs to
 * {@link TravelService} and is identical no matter what kind of destination this is. That split is what lets a
 * cross-server destination drop in later as a new implementation rather than a rewrite: a local one teleports, a
 * remote one hands the player to the proxy, and the service cannot tell the difference.
 */
public interface Destination {

    /**
     * @return a stable identity for this destination, used for lookup and persistence
     */
    @NotNull Key key();

    /**
     * @return the name shown to players in the navigator and on arrival
     */
    @NotNull Component displayName();

    /**
     * @return the icon shown for this destination in a navigator menu
     */
    @NotNull ItemView icon();

    /**
     * Whether this destination can currently accept travellers. A local destination whose world is not loaded, or a
     * remote one whose server is down, reports {@code false} and is filtered out of the navigator rather than failing
     * mid-voyage.
     *
     * @return whether travel here can be attempted right now
     */
    boolean isReady();

    /**
     * Relocates the traveller to this destination. Called by {@link TravelService} once every guard has passed and the
     * departure ceremony has finished.
     *
     * @param traveller the player to relocate
     * @return completes with {@code true} once the player has arrived, or {@code false} if the relocation failed
     */
    @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller);
}
