package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.LongIterator;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Whether a structure can stand somewhere: every column it covers inside a build zone carrying the tag it needs, and no
 * column shared with another structure at the same height. The same check serves a ghost following the player, a
 * placement, a move and an upgrade, for any position and rotation.
 */
@Singleton
public class FitCheck {

    /** What every build zone is tagged with. */
    public static final String BUILD_ZONE = "build_zone";

    private final ZoneManager zones;
    private final StructureShapes shapes;

    @Inject
    public FitCheck(@NotNull ZoneManager zones, @NotNull StructureShapes shapes) {
        this.zones = zones;
        this.shapes = shapes;
    }

    /**
     * @param ignoring a structure not to collide with, being the one that is moving or growing, or null
     * @return why it does not fit, or empty if it does
     */
    public @NotNull Optional<Component> problem(@NotNull World world, @NotNull Holding holding,
                                                @NotNull StructureType type, @NotNull SchematicPlacement placement,
                                                @Nullable UUID ignoring) {
        final Footprint footprint = placement.getFootprint();
        if (!insideBuildZones(world, footprint, type.getRequiredZoneTag())) {
            return Optional.of(type.getRequiredZoneTag() == null
                    ? Component.text("It has to go entirely inside a build zone.", NamedTextColor.RED)
                    : Component.text("It has to go entirely inside a " + type.getRequiredZoneTag().replace('_', ' ')
                    + " build zone.", NamedTextColor.RED));
        }

        for (Footprint other : occupied(world, holding, ignoring)) {
            if (footprint.intersects(other)) {
                return Optional.of(Component.text("It would overlap another structure.", NamedTextColor.RED));
            }
        }
        return Optional.empty();
    }

    private boolean insideBuildZones(@NotNull World world, @NotNull Footprint footprint, @Nullable String requiredTag) {
        if (footprint.isEmpty()) {
            return false;
        }
        final LongIterator columns = footprint.getColumns().iterator();
        while (columns.hasNext()) {
            final long column = columns.nextLong();
            final double x = Footprint.unpackX(column) + 0.5;
            final double z = Footprint.unpackZ(column) + 0.5;
            if (!inBuildZone(new Location(world, x, footprint.getMinY() + 0.5, z), requiredTag)
                    || !inBuildZone(new Location(world, x, footprint.getMaxY() + 0.5, z), requiredTag)) {
                return false;
            }
        }
        return true;
    }

    private boolean inBuildZone(@NotNull Location location, @Nullable String requiredTag) {
        for (Zone zone : zones.getZonesAt(location)) {
            if (zone.hasTag(BUILD_ZONE) && (requiredTag == null || zone.hasTag(requiredTag))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Everything the holding's other structures take up: where each stands, plus where a move is taking it and the
     * bigger version an upgrade is growing it into, since that ground is spoken for.
     */
    private @NotNull List<Footprint> occupied(@NotNull World world, @NotNull Holding holding, @Nullable UUID ignoring) {
        final List<Footprint> occupied = new ArrayList<>();
        for (PlacedStructure structure : holding.getStructures()) {
            if (structure.getId().equals(ignoring) || structure.getCondition() == StructureCondition.NOT_PLACED) {
                continue;
            }
            shapes.footprintOf(world, structure.getType(), structure.getVersion(), structure.getPosition())
                    .ifPresent(occupied::add);

            final Job job = structure.getJob();
            if (job != null && job.getKind() == JobKind.MOVE && job.getTarget() != null) {
                shapes.footprintOf(world, structure.getType(), structure.getVersion(), job.getTarget())
                        .ifPresent(occupied::add);
            }
            if (job != null && job.getKind() == JobKind.UPGRADE) {
                shapes.footprintOf(world, structure.getType(), job.getTargetVersion(), structure.getPosition())
                        .ifPresent(occupied::add);
            }
        }
        return occupied;
    }
}
