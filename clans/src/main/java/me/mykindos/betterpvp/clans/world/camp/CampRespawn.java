package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member who dies in their own camp comes back at its Barracks, on the build's {@code respawn} point. Without a
 * working Barracks they come back at the camp's spawn. Anyone else respawns wherever they otherwise would.
 */
@BPvPListener
@Singleton
public class CampRespawn implements Listener {

    public static final String MARKER = "respawn";

    private final Camps camps;
    private final ConstructionService construction;
    private final StructureShapes shapes;

    @Inject
    public CampRespawn(@NotNull Camps camps, @NotNull ConstructionService construction,
                       @NotNull StructureShapes shapes) {
        this.camps = camps;
        this.construction = construction;
        this.shapes = shapes;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Location death = player.getLastDeathLocation();
        if (death == null || death.getWorld() == null || !camps.isMember(player, death.getWorld())) {
            return;
        }
        event.setRespawnLocation(barracks(death.getWorld()).orElseGet(() -> death.getWorld().getSpawnLocation()));
    }

    /** Where a working Barracks in {@code world} puts respawning members. */
    public @NotNull Optional<Location> barracks(@NotNull World world) {
        final long now = construction.now();
        return construction.worksite(world).stream()
                .flatMap(worksite -> worksite.getHolding().getStructures().stream())
                .filter(structure -> structure.getType().equals(CampStructures.BARRACKS))
                .filter(structure -> structure.status(now).isUsable())
                .findFirst()
                .flatMap(structure -> point(world, structure));
    }

    private @NotNull Optional<Location> point(@NotNull World world, @NotNull PlacedStructure structure) {
        return shapes.placementOf(world, structure).flatMap(placement -> {
            for (Region marker : placement.markers()) {
                if (MARKER.equalsIgnoreCase(marker.getName()) && marker instanceof PointRegion point) {
                    return Optional.of(point.getLocation().clone());
                }
            }
            return Optional.empty();
        });
    }
}
