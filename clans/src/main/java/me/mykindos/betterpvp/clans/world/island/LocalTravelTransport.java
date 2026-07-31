package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Delivers a player to an {@link IslandHandle} by teleporting them to its world's spawn point directly. Assumes the
 * handle's world is already loaded on this server.
 */
@CustomLog
@Singleton
public class LocalTravelTransport implements TravelTransport {

    @Override
    public @NotNull CompletableFuture<Boolean> deliver(@NotNull Player traveller, @NotNull IslandHandle handle) {
        final World world = Bukkit.getWorld(handle.getWorld());
        if (world == null) {
            log.warn("Cannot deliver {} to island handle {} - world '{}' is not loaded", traveller.getName(), handle.getInstanceId(), handle.getWorld()).submit();
            return CompletableFuture.completedFuture(false);
        }

        return traveller.teleportAsync(world.getSpawnLocation());
    }

}
