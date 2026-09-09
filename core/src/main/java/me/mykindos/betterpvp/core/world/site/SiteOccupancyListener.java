package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
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

    @Inject
    public SiteOccupancyListener(@NotNull SiteInstances instances) {
        this.instances = instances;
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
     * Counts in a player who logs in inside a live instance. Where they log in at all is {@link Residency}'s, which
     * runs before this and has already moved anybody whose instance is gone.
     */
    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final World world = player.getWorld();
        if (!world.getName().startsWith(SiteWorlds.WORLD_ROOT)) {
            return;
        }

        instances.byWorld(world.getName())
                .filter(instance -> instance.getState() == SiteInstance.State.READY)
                .ifPresent(instance -> instances.enter(instance, player.getUniqueId()));
    }

    private void release(@NotNull Player player) {
        instances.byWorld(player.getWorld().getName())
                .ifPresent(instance -> instances.exit(instance, player.getUniqueId()));
    }
}
