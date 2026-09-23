package me.mykindos.betterpvp.clans.world.camp.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.PointRegion;
import dev.brauw.mapper.region.Region;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.core.world.construction.Holding;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a camp's resource chests are and how much they hold. A resource chest is a Mapper point named
 * {@code resource_chest} captured in a structure's build, sitting on the chest block, so each version of a structure
 * decides how many it has.
 * <p>
 * A structure's chests count once it has been built for the first time and while it stands, including while it is
 * being upgraded or moved, since its storage stays usable then.
 */
@Singleton
public class ResourceChests {

    public static final String MARKER = "resource_chest";

    private final StructureCatalogue catalogue;
    private final SchematicService schematics;
    private final StructureShapes shapes;
    private final CampConfig config;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> positions = new ConcurrentHashMap<>();

    @Inject
    public ResourceChests(@NotNull StructureCatalogue catalogue, @NotNull SchematicService schematics,
                          @NotNull StructureShapes shapes, @NotNull CampConfig config) {
        this.catalogue = catalogue;
        this.schematics = schematics;
        this.shapes = shapes;
        this.config = config;
    }

    /** How much everything in {@code holding} can hold, across all resources together. */
    public int capacity(@NotNull Holding holding) {
        int chests = 0;
        for (PlacedStructure structure : holding.getStructures()) {
            if (counts(structure)) {
                chests += count(structure);
            }
        }
        return chests * config.getChestCapacity();
    }

    /** The structure whose resource chest {@code block} is, if it is one. */
    public @NotNull Optional<PlacedStructure> at(@NotNull Holding holding, @NotNull World world, @NotNull Block block) {
        final long clicked = pack(block.getX(), block.getY(), block.getZ());
        return holding.getStructures().stream()
                .filter(ResourceChests::counts)
                .filter(structure -> positions(world, structure).contains(clicked))
                .findFirst();
    }

    private int count(@NotNull PlacedStructure structure) {
        final String key = structure.getType() + ":" + structure.getVersion();
        return counts.computeIfAbsent(key, unused -> catalogue.find(structure.getType())
                .flatMap(type -> schematics.load(type.version(structure.getVersion()).getSchematic()))
                .map(schematic -> (int) schematic.getRegions().stream()
                        .filter(region -> MARKER.equalsIgnoreCase(region.getName()))
                        .count())
                .orElse(0));
    }

    private @NotNull Set<Long> positions(@NotNull World world, @NotNull PlacedStructure structure) {
        final String key = world.getName() + ":" + structure.getId() + ":" + structure.getVersion() + ":"
                + structure.getPosition();
        return positions.computeIfAbsent(key, unused -> {
            final Set<Long> found = new HashSet<>();
            shapes.placementOf(world, structure).ifPresent(placement -> {
                for (Region marker : placement.markers()) {
                    if (MARKER.equalsIgnoreCase(marker.getName()) && marker instanceof PointRegion point) {
                        found.add(pack(point.getLocation().getBlockX(), point.getLocation().getBlockY(),
                                point.getLocation().getBlockZ()));
                    }
                }
            });
            return found;
        });
    }

    private static boolean counts(@NotNull PlacedStructure structure) {
        return structure.getCondition() != StructureCondition.UNDER_CONSTRUCTION
                && structure.getCondition() != StructureCondition.NOT_PLACED;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
