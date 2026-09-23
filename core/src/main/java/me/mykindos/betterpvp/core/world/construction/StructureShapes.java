package me.mykindos.betterpvp.core.world.construction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.schematic.Footprint;
import me.mykindos.betterpvp.core.world.schematic.SchematicPlacement;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a structure's build lands for a given stage and position. Footprints are kept once worked out, since the fit
 * check asks for every other structure's footprint each time a ghost moves.
 */
@Singleton
public class StructureShapes {

    private final StructureCatalogue catalogue;
    private final SchematicService schematics;
    private final Map<String, Footprint> footprints = new ConcurrentHashMap<>();

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

    public @NotNull Optional<Footprint> footprintOf(@NotNull World world, @NotNull String type, int stage,
                                                    @NotNull StructurePosition position) {
        final String key = type + ":" + stage + ":" + position.getX() + ":" + position.getY() + ":" + position.getZ()
                + ":" + position.getQuarterTurns();
        final Footprint known = footprints.get(key);
        if (known != null) {
            return Optional.of(known);
        }
        return placementOf(world, type, stage, position).map(placement -> {
            footprints.put(key, placement.getFootprint());
            return placement.getFootprint();
        });
    }

    /** Forgets every footprint, for when the builds behind them are reloaded. */
    public void clear() {
        footprints.clear();
    }
}
