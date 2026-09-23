package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.fatigue.RespawnHoldService;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.site.Party;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteLandings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Every clan member respawns at their own camp's Barracks, wherever they died, working or not. A camp loaded on this
 * server takes them straight there. Otherwise they come back at the normal spawn and are sent on to their camp,
 * landing at the Barracks, which is also where they stay if the camp cannot be reached. Players without a clan
 * respawn as they otherwise would.
 */
@CustomLog
@BPvPListener
@Singleton
public class CampRespawn implements Listener {

    /** The point in a Barracks build members respawn on. */
    public static final String MARKER = "respawn";
    /** The landing a respawning member travels to. */
    public static final String LANDING = "barracks";

    private final Clans clans;
    private final ClanManager clanManager;
    private final ConstructionService construction;
    private final StructureShapes shapes;
    private final SiteInstances instances;
    private final Placement placement;
    private final RespawnHoldService hold;

    @Inject
    public CampRespawn(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull ConstructionService construction,
                       @NotNull StructureShapes shapes, @NotNull SiteInstances instances, @NotNull Placement placement,
                       @NotNull RespawnHoldService hold, @NotNull SiteLandings landings) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.construction = construction;
        this.shapes = shapes;
        this.instances = instances;
        this.placement = placement;
        this.hold = hold;
        landings.register(Camps.SITE_ID, LANDING, this::barracks);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Optional<Clan> clan = clanManager.getClanByPlayer(player);
        if (clan.isEmpty()) {
            return;
        }

        final SiteKey key = Camps.keyFor(clan.get());
        final Optional<Location> here = instances.forKey(key).stream()
                .map(instance -> Bukkit.getWorld(instance.getWorldName()))
                .filter(Objects::nonNull)
                .findFirst()
                .flatMap(this::barracks);
        if (here.isPresent()) {
            event.setRespawnLocation(here.get());
            return;
        }
        travel(player, key);
    }

    /** Sends a respawned member to their camp's Barracks, once any respawn hold has let them go. */
    private void travel(@NotNull Player player, @NotNull SiteKey key) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (hold.isHeld(player)) {
                    return;
                }
                cancel();
                placement.locate(key, Party.solo(player.getUniqueId()))
                        .thenCompose(handle -> placement.send(player, handle, LANDING))
                        .exceptionally(error -> {
                            log.warn("Could not send {} to their camp's Barracks after respawning", player.getName(),
                                    error).submit();
                            return false;
                        });
            }
        }.runTaskTimer(clans, 1L, 20L);
    }

    /** Where the Barracks in {@code world} puts respawning members, broken or not. */
    public @NotNull Optional<Location> barracks(@NotNull World world) {
        return construction.worksite(world).stream()
                .flatMap(worksite -> worksite.getHolding().getStructures().stream())
                .filter(structure -> structure.getType().equals(CampStructures.BARRACKS))
                .filter(structure -> structure.getCondition() != StructureCondition.NOT_PLACED)
                .findFirst()
                .map(structure -> point(world, structure)
                        .orElseGet(() -> structure.getPosition().toLocation(world).add(0.5, 1, 0.5)));
    }

    private @NotNull Optional<Location> point(@NotNull World world, @NotNull PlacedStructure structure) {
        return shapes.placementOf(world, structure).flatMap(placed -> {
            for (Region marker : placed.markers()) {
                if (MARKER.equalsIgnoreCase(marker.getName()) && marker instanceof PointRegion point) {
                    return Optional.of(point.getLocation().clone());
                }
            }
            return Optional.empty();
        });
    }
}
