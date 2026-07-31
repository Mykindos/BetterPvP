package me.mykindos.betterpvp.clans.world.island;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Decides how a player physically gets to an {@link IslandHandle}. A local implementation teleports directly; a
 * future remote implementation would hand the player off to the proxy toward {@link IslandHandle#getServer()}
 * instead. This varies independently of {@link IslandAllocator} — delivery cares only about the server a handle
 * names, not about how that handle was allocated.
 */
public interface TravelTransport {

    @NotNull CompletableFuture<Boolean> deliver(@NotNull Player traveller, @NotNull IslandHandle handle);

}
