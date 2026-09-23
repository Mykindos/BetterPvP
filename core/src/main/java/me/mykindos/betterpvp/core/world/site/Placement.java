package me.mykindos.betterpvp.core.world.site;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The only thing that knows another server exists. Finds a party an instance wherever it lives, and puts a player in
 * it. Everything above this works the same on one server as on twenty.
 */
public interface Placement {

    /** Which server holds a site, being the current one unless its policy says otherwise. */
    @NotNull String hostFor(@NotNull Site site);

    boolean isLocal(@NotNull Site site);

    /** Finds the instance a party should travel to, which may be on another server. */
    @NotNull CompletableFuture<SiteHandle> locate(@NotNull SiteKey key, @NotNull Party party);

    /** Puts a player in an instance, by teleport or by handing them to another server. */
    default @NotNull CompletableFuture<Boolean> send(@NotNull Player traveller, @NotNull SiteHandle handle) {
        return send(traveller, handle, null);
    }

    /** As {@link #send(Player, SiteHandle)}, landing at a {@link SiteLandings named landing} when there is one. */
    default @NotNull CompletableFuture<Boolean> send(@NotNull Player traveller, @NotNull SiteHandle handle,
                                                     @Nullable String landing) {
        return sendAll(List.of(traveller), handle, landing);
    }

    /**
     * Puts a whole party in an instance. Where in it they land is decided once for everybody, so a site that scatters
     * its arrivals does not scatter a group that travelled together.
     */
    default @NotNull CompletableFuture<Boolean> sendAll(@NotNull Collection<Player> travellers,
                                                        @NotNull SiteHandle handle) {
        return sendAll(travellers, handle, null);
    }

    /**
     * As {@link #sendAll(Collection, SiteHandle)}, landing at {@code landing} instead of the site's arrival point.
     * A landing the destination cannot resolve falls back to the arrival point.
     */
    @NotNull CompletableFuture<Boolean> sendAll(@NotNull Collection<Player> travellers, @NotNull SiteHandle handle,
                                                @Nullable String landing);

}
