package me.mykindos.betterpvp.clans.world.sailing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.ship.Berth;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.TransitTiming;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs voyages: moves a crew into the staging world, rolls for arrival, and hands them to their destination together.
 * <p>
 * The destination resolves where they are going, {@link Ocean} owns the staging world, {@link Sailors} performs the
 * teleports and {@link VoyageCues} renders the feedback. What is left here is the lifecycle of a voyage.
 *
 * @see TransitTiming for the arrival schedule
 */
@BPvPListener
@Singleton
@CustomLog
public class VoyageService implements Listener {

    private final Ocean ocean;
    private final CrewService crewService;
    private final Sailors sailors;
    private final VoyageCues cues;

    /** Captains whose voyage is being started. Allocating a world is async, so two clicks would start two voyages. */
    private final Set<UUID> opening = ConcurrentHashMap.newKeySet();

    private final List<Voyage> voyages = new CopyOnWriteArrayList<>();

    @Inject
    public VoyageService(@NotNull Ocean ocean, @NotNull CrewService crewService, @NotNull Sailors sailors,
                         @NotNull VoyageCues cues) {
        this.ocean = ocean;
        this.crewService = crewService;
        this.sailors = sailors;
        this.cues = cues;
    }

    /**
     * Starts a voyage for {@code crew}. The roster is fixed from this point: no member is added or removed, and leaving
     * the ship no longer changes anything, because the destination is already chosen.
     */
    public @NotNull CompletableFuture<Boolean> begin(@NotNull Crew crew, @NotNull Landfall destination) {
        // Starting a voyage waits on a world clone, so a second click arrives before the first has any visible
        // effect. Without this guard the captain gets two staging worlds and the crew is split between them.
        if (!opening.add(crew.getCaptain())) {
            return CompletableFuture.completedFuture(false);
        }

        return ocean.claim().thenApply(water -> {
            crewService.depart(crew);
            final Voyage voyage = new Voyage(crew, destination, destination.timing(), water.getName(),
                    System.currentTimeMillis());
            voyages.add(voyage);

            try {
                // A voyage that could not move its crew must not stay registered. Left rolling, it would teleport
                // players to the destination a minute later from wherever they happened to be.
                if (!setSail(voyage)) {
                    abandon(voyage);
                    return false;
                }
                return true;
            } catch (RuntimeException exception) {
                // Anything thrown past here leaves the crew marked as travelling with no voyage to clear the flag,
                // so the helm refuses them, the ship will not re-form the crew, and nothing resets the state.
                log.error("Could not start a voyage for crew {}", crew.getCaptain(), exception).submit();
                abandon(voyage);
                return false;
            } finally {
                opening.remove(crew.getCaptain());
            }
        }).exceptionally(throwable -> {
            opening.remove(crew.getCaptain());
            log.error("Could not allocate a staging world for crew {}", crew.getCaptain(), throwable).submit();
            sailors.aboard(crew).forEach(cues::castOffFailed);
            return false;
        });
    }

    /** Teleports the crew into the staging world, onto the ship they departed from. */
    private boolean setSail(@NotNull Voyage voyage) {
        final World water = Bukkit.getWorld(voyage.getOcean());
        if (water == null) {
            log.warn("Staging world '{}' was gone before the crew could be moved into it", voyage.getOcean()).submit();
            return false;
        }

        final Optional<Berth> moored = ocean.moor(water, voyage.getCrew());
        if (moored.isEmpty()) {
            return false;
        }

        final Location deck = moored.get().getBoard();
        for (Player sailor : sailors.aboard(voyage.getCrew())) {
            sailors.move(sailor, deck);
            cues.castOff(sailor, voyage.getDestination().displayName());
        }
        return true;
    }

    /** Cancels a voyage that never started, releasing the staging world and leaving the crew where they are. */
    private void abandon(@NotNull Voyage voyage) {
        voyages.remove(voyage);
        sailors.aboard(voyage.getCrew()).forEach(cues::castOffFailed);

        crewService.disband(voyage.getCrew());
        ocean.release(voyage.getOcean());
    }

    /**
     * One arrival roll per voyage, every ten seconds. Rolled per voyage rather than per player so that a crew always
     * arrives at the same moment and in one group.
     */
    @UpdateEvent(delay = 10_000)
    public void rollForLandfall() {
        final long now = System.currentTimeMillis();
        for (Voyage voyage : voyages) {
            if (voyage.arrives(now, ThreadLocalRandom.current().nextDouble())) {
                makeLandfall(voyage);
            }
        }
    }

    /** Hands the crew to their destination, and releases the staging world once they are all out of it. */
    private void makeLandfall(@NotNull Voyage voyage) {
        voyages.remove(voyage);

        final List<Player> crew = stillAtSea(voyage);
        crew.forEach(sailors::readyToDisembark);

        landfall(voyage, crew).whenComplete((landed, error) -> {
            if (error != null) {
                log.error("Arrival for crew {} failed", voyage.getCrew().getCaptain(), error).submit();
            }

            crewService.disband(voyage.getCrew());
            ocean.release(voyage.getOcean());
        });
    }

    /**
     * Runs the arrival: the destination accepts the crew, or they are returned to their origin.
     * <p>
     * The staging world is not released until this completes. Deleting a world evacuates anyone still inside it to the
     * server's fallback world, which would race an in-flight teleport and drop the crew at spawn instead of at the
     * destination.
     *
     * @return completes once every player has left the staging world, by either route
     */
    private @NotNull CompletableFuture<Boolean> landfall(@NotNull Voyage voyage, @NotNull List<Player> crew) {
        final CompletableFuture<Boolean> ashore;
        try {
            ashore = voyage.getDestination().setAshore(crew);
        } catch (RuntimeException exception) {
            // Thrown rather than returned, so nothing downstream would observe it, and the staging world these
            // players are standing in is released here or not at all.
            return CompletableFuture.failedFuture(exception);
        }

        return ashore.thenCompose(landed -> {
            if (Boolean.TRUE.equals(landed)) {
                crew.forEach(sailor -> cues.landfall(sailor, voyage.getDestination().displayName()));
                return CompletableFuture.completedFuture(true);
            }

            // The destination refused them and the world they are standing in is about to be deleted. Returning them
            // to their origin is the only defined outcome: left in place they would be evacuated to whatever world
            // the server falls back to.
            final List<CompletableFuture<Boolean>> back = new ArrayList<>();
            for (Player sailor : crew) {
                cues.noHarbour(sailor);
                back.add(sailors.move(sailor, sailors.turnBack(sailor)));
            }
            return CompletableFuture.allOf(back.toArray(CompletableFuture[]::new)).thenApply(ignored -> false);
        });
    }

    private @NotNull List<Player> stillAtSea(@NotNull Voyage voyage) {
        return sailors.stillIn(voyage.getCrew(), voyage.getOcean());
    }

    /** The voyage {@code player} is currently on, if any. */
    public @NotNull Optional<Voyage> voyageOf(@NotNull UUID player) {
        return voyages.stream().filter(voyage -> voyage.getCrew().has(player)).findFirst();
    }

    @UpdateEvent(delay = 1000)
    public void showVoyageClock() {
        final long now = System.currentTimeMillis();
        for (Voyage voyage : voyages) {
            for (Player sailor : stillAtSea(voyage)) {
                cues.clock(sailor, voyage.getDestination().displayName(), voyage.elapsedSeconds(now));
            }
        }
    }

    /**
     * Ends one player's voyage when anything else teleports them out of the staging world, such as a home command or
     * an admin.
     * <p>
     * This service's own teleports are excluded. Counting those as the player leaving would cancel every voyage at the
     * moment it succeeded.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportOut(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (sailors.isOurs(player) || event.getTo() == null) {
            return;
        }

        final Voyage voyage = voyageOf(player.getUniqueId()).orElse(null);
        if (voyage == null || !voyage.getOcean().equals(event.getFrom().getWorld().getName())) {
            return;
        }
        if (voyage.getOcean().equals(event.getTo().getWorld().getName())) {
            return; // still in the staging world, just moved within it
        }

        // This ends the voyage for that player only. The rest of the crew continues.
        crewService.leave(player.getUniqueId());
        cues.leftMidCrossing(player);
    }
}
