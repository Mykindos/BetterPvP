package me.mykindos.betterpvp.core.item.impl.cannon.listener;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.ride.CannonRideService;
import me.mykindos.betterpvp.core.item.impl.cannon.ride.RidePhase;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Wires the human-cannonball ride into the server tick and the player lifecycle. Kept separate from
 * {@link CannonListener} so the ride stays a feature layered on top of cannons rather than part of every cannon.
 */
@BPvPListener
@Singleton
public class CannonRideListener implements Listener {

    @Inject
    private CannonRideService rideService;

    @UpdateEvent
    public void tickRides() {
        rideService.tick();
    }

    /**
     * Puts back anyone who was mid-ride when they disconnected. Runs for every joining player, since the origin record
     * may have been written by a previous server process.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        rideService.restoreIfStranded(event.getPlayer());
        rideService.hideRestrictedFrom(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        rideService.onQuit(event.getPlayer().getUniqueId());
    }

    /** A rider is a spectator watching a puppet - nothing should be able to hurt them until they land. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (rideService.isRiding(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Keeps the rider's camera on the mannequin. Sneaking is the vanilla way to stop spectating an entity, which
     * mid-flight would leave them free-flying as a spectator with a ride still running. The service marks a ride
     * finished before releasing, so its own un-spectate is not blocked here.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        rideService.of(event.getPlayer().getUniqueId())
                .filter(ride -> ride.getPhase() != RidePhase.FINISHED)
                .ifPresent(ride -> event.setCancelled(true));
    }

    /**
     * Blocks anything that would move a rider out from under their own camera mid-flight. A teleport detaches the
     * spectator camera, leaving them adrift somewhere else until the flight times out.
     * <p>
     * No cause is exempted, including {@code PLUGIN}: the ride's own teleports all run after {@code release()} has
     * marked the ride finished, so they never reach this check, while an unrelated {@code /tp}-style teleport would.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        rideService.of(event.getPlayer().getUniqueId())
                .filter(ride -> ride.getPhase() != RidePhase.FINISHED)
                .ifPresent(ride -> event.setCancelled(true));
    }
}
