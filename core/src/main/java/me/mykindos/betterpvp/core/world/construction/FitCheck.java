package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Whether a structure can stand somewhere: every column it covers inside a build zone carrying the tag it needs, and its
 * bounds at least {@link #CLEARANCE} blocks from every other structure's bounds. The same check serves a ghost following
 * the player, a placement, a move and an advance, for any position and rotation.
 */
@Singleton
public class FitCheck {

    /** What every build zone is tagged with. */
    public static final String BUILD_ZONE = "build_zone";
    /** How many blocks of open ground a structure's bounds keep from another's on every side. */
    public static final int CLEARANCE = 5;

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
            final String tag = type.getRequiredZoneTag();
            return Optional.of((tag == null
                    ? Translations.component("core.construction.outside_build_zone")
                    : Translations.component("core.construction.outside_tagged_build_zone", zoneTag(tag)))
                    .color(NamedTextColor.RED));
        }

        final BoundingBox bounds = placement.selectionBounds();
        if (nearby(bounds, occupied(world, holding, ignoring)).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tooClose(type));
    }

    /** Why {@code type} cannot stand where it is: too close to another structure. */
    public static @NotNull Component tooClose(@NotNull StructureType type) {
        return Translations.component("core.construction.too_close", type.getDisplayName().color(NamedTextColor.WHITE))
                .color(NamedTextColor.RED);
    }

    /**
     * The columns of {@code placement} that stop it fitting, packed with {@link Footprint#pack}: those outside the
     * build zones it needs and those within {@link #CLEARANCE} of another structure's bounds. When the bounds are too
     * close but none of the built columns is, every column clashes. Empty if it fits.
     *
     * @param ignoring a structure not to collide with, being the one that is moving or growing, or null
     */
    public @NotNull LongSet clashes(@NotNull World world, @NotNull Holding holding, @NotNull StructureType type,
                                    @NotNull SchematicPlacement placement, @Nullable UUID ignoring) {
        final Footprint footprint = placement.getFootprint();
        final BoundingBox bounds = placement.selectionBounds();
        final List<BoundingBox> nearby = nearby(bounds, occupied(world, holding, ignoring));
        final LongSet clashing = new LongOpenHashSet();
        boolean columnTooClose = false;
        final LongIterator columns = footprint.getColumns().iterator();
        while (columns.hasNext()) {
            final long column = columns.nextLong();
            final int x = Footprint.unpackX(column);
            final int z = Footprint.unpackZ(column);
            final BoundingBox cell = new BoundingBox(x, bounds.getMinY(), z, x + 1, bounds.getMaxY(), z + 1);
            final boolean tooClose = nearby.stream().anyMatch(cell::overlaps);
            columnTooClose |= tooClose;
            if (tooClose || !inBuildZones(world, footprint, column, type.getRequiredZoneTag())) {
                clashing.add(column);
            }
        }
        if (!nearby.isEmpty() && !columnTooClose) {
            clashing.addAll(footprint.getColumns());
        }
        return clashing;
    }

    /** The {@code others}, each grown by {@link #CLEARANCE}, that {@code bounds} runs into. */
    private static @NotNull List<BoundingBox> nearby(@NotNull BoundingBox bounds, @NotNull List<BoundingBox> others) {
        return others.stream()
                .map(other -> other.expand(CLEARANCE))
                .filter(bounds::overlaps)
                .toList();
    }

    /** A zone tag's name for players, falling back to the tag itself for one without a translation. */
    private static @NotNull Component zoneTag(@NotNull String tag) {
        return Component.translatable("core.construction.zone_tag." + tag, tag.replace('_', ' '));
    }

    private boolean insideBuildZones(@NotNull World world, @NotNull Footprint footprint, @Nullable String requiredTag) {
        if (footprint.isEmpty()) {
            return false;
        }
        final LongIterator columns = footprint.getColumns().iterator();
        while (columns.hasNext()) {
            if (!inBuildZones(world, footprint, columns.nextLong(), requiredTag)) {
                return false;
            }
        }
        return true;
    }

    /** Whether one column of {@code footprint} is inside the build zones at both its bottom and its top. */
    private boolean inBuildZones(@NotNull World world, @NotNull Footprint footprint, long column,
                                 @Nullable String requiredTag) {
        final double x = Footprint.unpackX(column) + 0.5;
        final double z = Footprint.unpackZ(column) + 0.5;
        return inBuildZone(new Location(world, x, footprint.getMinY() + 0.5, z), requiredTag)
                && inBuildZone(new Location(world, x, footprint.getMaxY() + 0.5, z), requiredTag);
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
     * The bounds of the holding's other structures: where each stands, plus where a move is taking it and the bigger
     * stage it is advancing into, since that ground is spoken for.
     */
    private @NotNull List<BoundingBox> occupied(@NotNull World world, @NotNull Holding holding, @Nullable UUID ignoring) {
        final List<BoundingBox> occupied = new ArrayList<>();
        for (PlacedStructure structure : holding.getStructures()) {
            if (structure.getId().equals(ignoring) || structure.getCondition() == StructureCondition.NOT_PLACED) {
                continue;
            }
            shapes.boundsOf(world, structure.getType(), structure.getStage(), structure.getPosition())
                    .ifPresent(occupied::add);

            final Job job = structure.getJob();
            if (job != null && job.getKind() == JobKind.MOVE && job.getTarget() != null) {
                shapes.boundsOf(world, structure.getType(), structure.getStage(), job.getTarget())
                        .ifPresent(occupied::add);
            }
            if (job != null && job.getKind() == JobKind.ADVANCE) {
                shapes.boundsOf(world, structure.getType(), job.getTargetStage(), structure.getPosition())
                        .ifPresent(occupied::add);
            }
        }
        return occupied;
    }
}
