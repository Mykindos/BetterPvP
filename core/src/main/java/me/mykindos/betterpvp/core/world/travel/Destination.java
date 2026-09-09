package me.mykindos.betterpvp.core.world.travel;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Somewhere a player can be sent by {@link TravelService}: a world that is always there, one instance of many, or a
 * place on another server.
 * <p>
 * A destination owns exactly one thing, which is getting the player to itself through {@link #receive(Player)}.
 * Everything around that, being the guards, the hold, the cues and recording where the player came from, belongs to
 * {@link TravelService} and is the same whatever kind of destination this is. That split is what lets a cross-server
 * destination arrive later as another implementation rather than a rewrite: a local one teleports, a remote one hands
 * the player to the proxy, and the service cannot tell the difference.
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
     * Whether this destination can accept anybody right now. A local one whose world is not loaded, or a remote one
     * whose server is down, answers {@code false} and is left out of the navigator rather than failing part way.
     *
     * @return whether travel here can be attempted right now
     */
    boolean isReady();

    /**
     * Relocates the player to this destination. Called by {@link TravelService} once every guard has passed and the
     * departure hold has finished.
     *
     * @param traveller the player to relocate
     * @return completes with {@code true} once the player has arrived, or {@code false} if the relocation failed
     */
    @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller);

    /**
     * Whether a successful {@link #receive(Player)} means the player is standing at this destination, and so should
     * be told they have arrived.
     * <p>
     * False for anything that only <em>starts</em> a journey. A destination reached minutes later succeeds as soon as
     * the journey begins, so announcing arrival there would tell somebody they had got somewhere they are still on
     * their way to.
     */
    default boolean announcesArrival() {
        return true;
    }
}
