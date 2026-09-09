package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.travel.ServerLocation;
import me.mykindos.betterpvp.core.world.travel.TravelHistory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Keeps who-is-where honest. An instance counts a player from the moment they walk into its world until they leave it,
 * quit or die, so nothing is ever held open by somebody who is not there.
 */
@CustomLog
@BPvPListener
@Singleton
public class SiteOccupancyListener implements Listener {

    private final SiteInstances instances;
    private final TravelHistory travelHistory;
    private final WorldHandler worldHandler;

    @Inject
    public SiteOccupancyListener(@NotNull SiteInstances instances, @NotNull TravelHistory travelHistory,
                                 @NotNull WorldHandler worldHandler) {
        this.instances = instances;
        this.travelHistory = travelHistory;
        this.worldHandler = worldHandler;
    }

    @EventHandler
    public void onChangedWorld(@NotNull PlayerChangedWorldEvent event) {
        instances.byWorld(event.getFrom().getName())
                .ifPresent(left -> instances.exit(left, event.getPlayer().getUniqueId()));

        instances.byWorld(event.getPlayer().getWorld().getName())
                .ifPresent(entered -> instances.enter(entered, event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        release(event.getPlayer());
    }

    @EventHandler
    public void onDeath(@NotNull PlayerDeathEvent event) {
        release(event.getEntity());
    }

    /**
     * Decides what happens to somebody who logs in inside a site's world. A live instance takes them back, since a
     * brief disconnect should not cost a run. One that is gone leaves them standing in a world nothing owns, so they
     * are sent back where they set out from.
     */
    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final World world = player.getWorld();
        if (!world.getName().startsWith(SiteWorlds.WORLD_ROOT)) {
            return;
        }

        final Optional<SiteInstance> instance = instances.byWorld(world.getName());
        if (instance.isPresent() && instance.get().getState() == SiteInstance.State.READY) {
            instances.enter(instance.get(), player.getUniqueId());
            return;
        }

        player.teleportAsync(travelHistory.origin(player)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation));
    }

    private void release(@NotNull Player player) {
        instances.byWorld(player.getWorld().getName())
                .ifPresent(instance -> instances.exit(instance, player.getUniqueId()));
    }
}
