package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.travel.ServerLocation;
import me.mykindos.betterpvp.clans.world.travel.TravelHistory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.WorldHandler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Decides what happens to a player who rejoins inside an island world.
 * <p>
 * If the instance is still live they are put back into it — a brief disconnect should not cost a run, which matters
 * because island resources are harvested once and cannot be recovered. Only when the instance is gone (reaped while
 * they were away, or destroyed by boot recovery after a restart) are they sent to their {@link TravelHistory} origin,
 * falling back to {@link WorldHandler#getSpawnLocation()}.
 */
@BPvPListener
@Singleton
public class IslandStrandedPlayerListener implements Listener {

    private final IslandInstanceManager instanceManager;
    private final TravelHistory travelHistory;
    private final WorldHandler worldHandler;

    @Inject
    public IslandStrandedPlayerListener(@NotNull IslandInstanceManager instanceManager,
                                        @NotNull TravelHistory travelHistory, @NotNull WorldHandler worldHandler) {
        this.instanceManager = instanceManager;
        this.travelHistory = travelHistory;
        this.worldHandler = worldHandler;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final World world = player.getWorld();

        if (world == null || !world.getName().startsWith("islands/")) {
            return;
        }

        final Optional<IslandInstance> instance = instanceManager.byWorld(world.getName());
        if (instance.isPresent() && instance.get().getState() == IslandInstanceState.READY) {
            // Re-registering occupancy also takes the instance back out of the reaper's reach.
            instanceManager.enter(instance.get(), player);
            return;
        }

        final Location destination = travelHistory.origin(player)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);

        player.teleport(destination);
    }

}
