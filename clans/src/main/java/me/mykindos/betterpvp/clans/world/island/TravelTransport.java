package me.mykindos.betterpvp.clans.world.island;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Decides how a player physically gets to an {@link IslandHandle}. Delivery across servers is {@code Placement}'s
 * job in core, so this only ever teleports.
 */
public interface TravelTransport {

    @NotNull CompletableFuture<Boolean> deliver(@NotNull Player traveller, @NotNull IslandHandle handle);

}
