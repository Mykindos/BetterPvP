package me.mykindos.betterpvp.clans.world.sailing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.core.scene.indicator.IndicatorService;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.travel.ServerLocation;
import me.mykindos.betterpvp.core.world.travel.TravelHistory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a {@link Crew} to online players and teleports them.
 * <p>
 * Every teleport a voyage performs crosses worlds, which needs two things handled every time: passengers must be
 * removed first, and the teleport must be distinguishable from one the player triggered themselves. Both are handled
 * here so {@link VoyageService} does not repeat them at each call site.
 */
@Singleton
@CustomLog
public class Sailors {

    private final IndicatorService indicators;
    private final TravelHistory travelHistory;
    private final WorldHandler worldHandler;

    /** Players currently being teleported by this class, checked by {@link #isOurs}. */
    private final Set<UUID> relocating = ConcurrentHashMap.newKeySet();

    @Inject
    public Sailors(@NotNull IndicatorService indicators, @NotNull TravelHistory travelHistory,
                   @NotNull WorldHandler worldHandler) {
        this.indicators = indicators;
        this.travelHistory = travelHistory;
        this.worldHandler = worldHandler;
    }

    /** The crew members who are online. Offline members are skipped rather than blocking the voyage. */
    public @NotNull List<Player> aboard(@NotNull Crew crew) {
        final List<Player> present = new ArrayList<>();
        for (UUID member : crew.roster()) {
            final Player online = Bukkit.getPlayer(member);
            if (online != null) {
                present.add(online);
            }
        }
        return present;
    }

    /**
     * The crew members who are online and still in {@code worldName}.
     * <p>
     * A player who has left that world by any other means has already ended their voyage, so teleporting them to the
     * destination later would pull them out of whatever they moved on to.
     */
    public @NotNull List<Player> stillIn(@NotNull Crew crew, @NotNull String worldName) {
        return aboard(crew).stream()
                .filter(player -> player.getWorld().getName().equals(worldName))
                .toList();
    }

    /**
     * Teleports a player and records it as ours for the duration, so {@link #isOurs} can distinguish it from a
     * teleport the player triggered.
     * <p>
     * A refused teleport is logged rather than ignored. A failed voyage looks identical to nothing happening, so this
     * is the only signal that anything went wrong.
     */
    public @NotNull CompletableFuture<Boolean> move(@NotNull Player player, @NotNull Location destination) {
        readyToDisembark(player);

        relocating.add(player.getUniqueId());
        return player.teleportAsync(destination).whenComplete((moved, error) -> {
            relocating.remove(player.getUniqueId());
            if (error != null) {
                log.warn("Could not move {} to {}", player.getName(), destination, error).submit();
            } else if (!Boolean.TRUE.equals(moved)) {
                log.warn("Move of {} to {} was refused", player.getName(), destination).submit();
            }
        });
    }

    /** Whether the teleport this player is currently making was started by {@link #move}. */
    public boolean isOurs(@NotNull Player player) {
        return relocating.contains(player.getUniqueId());
    }

    /**
     * Removes anything that would block a cross-world teleport.
     * <p>
     * Paper refuses to teleport a player carrying passengers to another world, and a crew captain carries one without
     * any visible sign of it: the crew indicator is mounted on them. Public rather than only used by {@link #move}
     * because a {@link Landfall} may teleport players itself, and an attached indicator would make that call return
     * false with no error.
     */
    public void readyToDisembark(@NotNull Player player) {
        indicators.detach(player);
        player.eject();
    }

    /** The location to return a player to when a voyage has no destination available: their origin, else spawn. */
    public @NotNull Location turnBack(@NotNull Player player) {
        return travelHistory.origin(player)
                .flatMap(ServerLocation::toLocation)
                .orElseGet(worldHandler::getSpawnLocation);
    }
}
