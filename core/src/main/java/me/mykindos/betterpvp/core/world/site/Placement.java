package me.mykindos.betterpvp.core.world.site;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    @NotNull CompletableFuture<Boolean> send(@NotNull Player traveller, @NotNull SiteHandle handle);

}
