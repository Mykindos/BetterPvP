package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Guarantees an {@link IslandInstance}'s occupancy never leaks: a player who quits or dies while standing on a
 * discovery island is removed from it immediately, regardless of where they end up afterwards.
 */
@CustomLog
@BPvPListener
@Singleton
public class IslandOccupancyListener implements Listener {

    private final IslandInstanceManager instanceManager;

    @Inject
    public IslandOccupancyListener(@NotNull IslandInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        release(event.getPlayer());
    }

    @EventHandler
    public void onDeath(@NotNull PlayerDeathEvent event) {
        release(event.getEntity());
    }

    private void release(@NotNull Player player) {
        instanceManager.byWorld(player.getWorld().getName()).ifPresent(instance -> {
            instanceManager.exit(instance, player);
            log.debug("Released island occupancy for {} on instance {}", player.getName(), instance.getId()).submit();
        });
    }

}
