package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a structure's build lands for a given stage and position. Footprints and bounds are kept once worked out,
 * since the fit check asks for every other structure's bounds each time a ghost moves, and worked out again once the
 * schematics behind them are reloaded.
 */
@Singleton
public class StructureShapes {

    private final StructureCatalogue catalogue;
    private final SchematicService schematics;
    private final Map<String, Footprint> footprints = new ConcurrentHashMap<>();
    private final Map<String, BoundingBox> bounds = new ConcurrentHashMap<>();
    private volatile long seen;

    @Inject
    public StructureShapes(@NotNull StructureCatalogue catalogue, @NotNull SchematicService schematics) {
        this.catalogue = catalogue;
        this.schematics = schematics;
    }

    /** The structure as it stands now. */
    public @NotNull Optional<SchematicPlacement> placementOf(@NotNull World world, @NotNull PlacedStructure structure) {
        return placementOf(world, structure.getType(), structure.getStage(), structure.getPosition());
    }

    public @NotNull Optional<SchematicPlacement> placementOf(@NotNull World world, @NotNull String type, int stage,
                                                             @NotNull StructurePosition position) {
        return catalogue.find(type)
                .flatMap(found -> schematics.load(found.stage(stage).getSchematic()))
                .map(schematic -> SchematicPlacement.of(schematic, position.toLocation(world), position.getQuarterTurns()));
    }

    /** The first point named {@code name} in the build a structure shows now, if its build has one. */
    public @NotNull Optional<Location> point(@NotNull World world, @NotNull PlacedStructure structure,
                                             @NotNull String name) {
        return placementOf(world, structure).flatMap(placed -> {
            for (Region marker : placed.markers()) {
                if (name.equalsIgnoreCase(marker.getName()) && marker instanceof PointRegion point) {
                    return Optional.of(point.getLocation().clone());
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Where an upgrade's piece lands on a structure as it stands now: on the upgrade's point in the structure's build,
     * turned the way the structure is. Empty if the upgrade has no piece or the build has no point for it.
     */
    public @NotNull Optional<SchematicPlacement> pieceOf(@NotNull World world, @NotNull PlacedStructure structure,
                                                         @NotNull StructureUpgrade upgrade) {
        final String piece = upgrade.getPiece();
        if (piece == null) {
            return Optional.empty();
        }
        return point(world, structure, upgrade.point()).flatMap(at -> schematics.load(piece)
                .map(schematic -> SchematicPlacement.of(schematic, at, structure.getPosition().getQuarterTurns())));
    }

    public @NotNull Optional<Footprint> footprintOf(@NotNull World world, @NotNull String type, int stage,
                                                    @NotNull StructurePosition position) {
        final String key = key(current(), type, stage, position);
        final Footprint known = footprints.get(key);
        if (known != null) {
            return Optional.of(known);
        }
        return placementOf(world, type, stage, position).map(placement -> {
            footprints.put(key, placement.getFootprint());
            return placement.getFootprint();
        });
    }

    /** The structure's bounds, the captured selection turned and moved to where it stands. */
    public @NotNull Optional<BoundingBox> boundsOf(@NotNull World world, @NotNull String type, int stage,
                                                   @NotNull StructurePosition position) {
        final String key = key(current(), type, stage, position);
        final BoundingBox known = bounds.get(key);
        if (known != null) {
            return Optional.of(known.clone());
        }
        return placementOf(world, type, stage, position).map(placement -> {
            final BoundingBox box = placement.selectionBounds();
            bounds.put(key, box.clone());
            return box;
        });
    }

    /**
     * The schematics' generation, dropping what was worked out under an older one. Keys carry it, so an entry from an
     * older generation is never read.
     */
    private long current() {
        final long generation = schematics.generation();
        if (generation != seen) {
            seen = generation;
            footprints.clear();
            bounds.clear();
        }
        return generation;
    }

    private static @NotNull String key(long generation, @NotNull String type, int stage,
                                       @NotNull StructurePosition position) {
        return generation + ":" + type + ":" + stage + ":" + position.getX() + ":" + position.getY() + ":"
                + position.getZ() + ":" + position.getQuarterTurns();
    }
}
